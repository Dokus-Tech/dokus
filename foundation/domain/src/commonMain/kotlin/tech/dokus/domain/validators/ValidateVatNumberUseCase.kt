package tech.dokus.domain.validators

import tech.dokus.domain.ids.VatNumber

/**
 * Validates VAT numbers with EU country-specific validation
 *
 * Implements validation for all EU member states according to their official formats:
 * - Structural validation (format/length)
 * - Check digit validation where applicable
 *
 * Belgian VAT validation algorithm:
 * - Format: BE + 10 digits (e.g., BE0123456789)
 * - Check digits: last 2 digits must equal 97 - (first 8 digits % 97)
 *
 * @see <a href="https://ec.europa.eu/taxation_customs/vies/faqvies.do">EU VIES FAQ</a>
 */
object ValidateVatNumberUseCase : Validator<VatNumber> {
    /** Modulo divisor for Belgian/mod-97 check digit algorithm */
    private const val MOD_97 = 97

    /** Country code prefix length */
    private const val COUNTRY_CODE_LENGTH = 2

    /**
     * EU VAT number patterns by country code
     * Format: Regex pattern for the digits/characters AFTER the country code
     */
    private val EU_VAT_PATTERNS: Map<String, Regex> = mapOf(
        // Austria: U + 8 digits (U12345678)
        "AT" to Regex("^U[0-9]{8}$"),
        // Belgium: 10 digits, first digit is 0 or 1 (0123456789)
        "BE" to Regex("^[01][0-9]{9}$"),
        // Bulgaria: 9 or 10 digits
        "BG" to Regex("^[0-9]{9,10}$"),
        // Croatia: 11 digits
        "HR" to Regex("^[0-9]{11}$"),
        // Cyprus: 8 digits + 1 letter
        "CY" to Regex("^[0-9]{8}[A-Z]$"),
        // Czech Republic: 8, 9, or 10 digits
        "CZ" to Regex("^[0-9]{8,10}$"),
        // Denmark: 8 digits
        "DK" to Regex("^[0-9]{8}$"),
        // Estonia: 9 digits
        "EE" to Regex("^[0-9]{9}$"),
        // Finland: 8 digits
        "FI" to Regex("^[0-9]{8}$"),
        // France: 2 chars (letters or digits) + 9 digits
        "FR" to Regex("^[A-Z0-9]{2}[0-9]{9}$"),
        // Germany: 9 digits
        "DE" to Regex("^[0-9]{9}$"),
        // Greece: 9 digits (also accepts EL prefix)
        "EL" to Regex("^[0-9]{9}$"),
        "GR" to Regex("^[0-9]{9}$"),
        // Hungary: 8 digits
        "HU" to Regex("^[0-9]{8}$"),
        // Ireland: 7 digits + 1-2 letters, or 1 digit + 1 letter/+ + 5 digits + 1 letter
        "IE" to Regex("^([0-9]{7}[A-Z]{1,2}|[0-9][A-Z+*][0-9]{5}[A-Z])$"),
        // Italy: 11 digits
        "IT" to Regex("^[0-9]{11}$"),
        // Latvia: 11 digits
        "LV" to Regex("^[0-9]{11}$"),
        // Lithuania: 9 or 12 digits
        "LT" to Regex("^([0-9]{9}|[0-9]{12})$"),
        // Luxembourg: 8 digits
        "LU" to Regex("^[0-9]{8}$"),
        // Malta: 8 digits
        "MT" to Regex("^[0-9]{8}$"),
        // Netherlands: 12 chars (9 digits + B + 2 digits)
        "NL" to Regex("^[0-9]{9}B[0-9]{2}$"),
        // Poland: 10 digits
        "PL" to Regex("^[0-9]{10}$"),
        // Portugal: 9 digits
        "PT" to Regex("^[0-9]{9}$"),
        // Romania: 2-10 digits
        "RO" to Regex("^[0-9]{2,10}$"),
        // Slovakia: 10 digits
        "SK" to Regex("^[0-9]{10}$"),
        // Slovenia: 8 digits
        "SI" to Regex("^[0-9]{8}$"),
        // Spain: letter + 7 digits + letter, or letter + 8 digits, or 8 digits + letter
        "ES" to Regex("^([A-Z][0-9]{7}[A-Z]|[A-Z][0-9]{8}|[0-9]{8}[A-Z])$"),
        // Sweden: 12 digits
        "SE" to Regex("^[0-9]{12}$"),
        // Northern Ireland (post-Brexit, uses XI prefix): same as UK format
        "XI" to Regex("^([0-9]{9}|[0-9]{12}|GD[0-9]{3}|HA[0-9]{3})$"),
    )

    override operator fun invoke(value: VatNumber): Boolean {
        if (value.value.isBlank()) return false

        val cleaned = value.value
            .replace(".", "")
            .replace(" ", "")
            .replace("-", "")
            .uppercase()

        // Must have at least country code + 1 character
        if (cleaned.length < 3) return false

        val countryCode = cleaned.substring(0, COUNTRY_CODE_LENGTH)
        val vatBody = cleaned.substring(COUNTRY_CODE_LENGTH)

        // Check if it's a known EU country code
        val pattern = EU_VAT_PATTERNS[countryCode]
            ?: return false // Unknown country code - reject

        // Check structural format
        if (!vatBody.matches(pattern)) return false

        // Apply country-specific check digit validation
        return when (countryCode) {
            "BE" -> validateBelgianCheckDigits(vatBody)
            "DE" -> validateGermanCheckDigit(vatBody)
            "NL" -> validateDutchCheckDigit(vatBody)
            "IT" -> validateItalianCheckDigit(vatBody)
            "ES" -> validateSpanishCheckDigit(vatBody)
            "PT" -> validatePortugueseCheckDigit(vatBody)
            "FI" -> validateFinnishCheckDigit(vatBody)
            "LU" -> validateLuxembourgCheckDigit(vatBody)
            "PL" -> validatePolishCheckDigit(vatBody)
            "SI" -> validateSlovenianCheckDigit(vatBody)
            "AT" -> validateAustrianCheckDigit(vatBody)
            else -> true // Format valid, no check digit algorithm implemented
        }
    }

    /**
     * Belgian VAT: last 2 digits = 97 - (first 8 digits % 97)
     */
    private fun validateBelgianCheckDigits(vatBody: String): Boolean {
        val baseNumber = vatBody.substring(0, 8).toLongOrNull() ?: return false
        val checkDigits = vatBody.substring(8).toIntOrNull() ?: return false
        val expected = MOD_97 - (baseNumber % MOD_97).toInt()
        return checkDigits == expected
    }

    /**
     * German VAT: Uses ISO 7064 MOD 11-10 algorithm
     */
    private fun validateGermanCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 9) return false

        var product = 10
        for (i in 0 until 8) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            var sum = (digit + product) % 10
            if (sum == 0) sum = 10
            product = (2 * sum) % 11
        }

        val checkDigit = vatBody[8].digitToIntOrNull() ?: return false
        val expected = (11 - product) % 10
        return checkDigit == expected
    }

    /**
     * Dutch VAT: Weighted sum mod 11, check digit is position 9
     * Format: 9 digits + B + 2 digits, where digit 9 is the check
     */
    private fun validateDutchCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 12 || vatBody[9] != 'B') return false

        val weights = listOf(9, 8, 7, 6, 5, 4, 3, 2, 1)
        var sum = 0
        for (i in 0 until 9) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += digit * weights[i]
        }

        return sum % 11 == 0
    }

    /**
     * Italian VAT: Luhn algorithm variant
     */
    private fun validateItalianCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 11) return false

        var sum = 0
        for (i in 0 until 10) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += if (i % 2 == 0) {
                digit
            } else {
                val doubled = digit * 2
                if (doubled > 9) doubled - 9 else doubled
            }
        }

        val checkDigit = vatBody[10].digitToIntOrNull() ?: return false
        val expected = (10 - (sum % 10)) % 10
        return checkDigit == expected
    }

    /**
     * Spanish VAT: Complex algorithm depending on first character
     */
    private fun validateSpanishCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 9) return false

        val first = vatBody[0]
        val last = vatBody[8]
        val middle = vatBody.substring(1, 8)

        // Calculate weighted sum
        val weights = listOf(2, 1, 2, 1, 2, 1, 2)
        var sum = 0
        for (i in 0 until 7) {
            val digit = middle[i].digitToIntOrNull() ?: return false
            val product = digit * weights[i]
            sum += if (product > 9) product - 9 else product
        }

        val control = (10 - (sum % 10)) % 10
        val controlLetter = "JABCDEFGHI"[control]

        return when {
            // Natural persons (starts with letter K, L, M, X, Y, Z or digit)
            first.isDigit() -> last == controlLetter || last.digitToIntOrNull() == control
            first in "KLMXYZ" -> last == controlLetter
            // Legal entities (starts with A-H, N, P-S, W)
            else -> last == controlLetter || last.digitToIntOrNull() == control
        }
    }

    /**
     * Portuguese VAT: Weighted sum mod 11
     */
    private fun validatePortugueseCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 9) return false

        val weights = listOf(9, 8, 7, 6, 5, 4, 3, 2, 1)
        var sum = 0
        for (i in 0 until 9) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += digit * weights[i]
        }

        return sum % 11 == 0
    }

    /**
     * Finnish VAT: Weighted sum mod 11
     */
    private fun validateFinnishCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 8) return false

        val weights = listOf(7, 9, 10, 5, 8, 4, 2)
        var sum = 0
        for (i in 0 until 7) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += digit * weights[i]
        }

        val remainder = sum % 11
        val expected = if (remainder == 0) 0 else 11 - remainder
        if (expected == 10) return false // Invalid VAT number

        val checkDigit = vatBody[7].digitToIntOrNull() ?: return false
        return checkDigit == expected
    }

    /**
     * Luxembourg VAT: last 2 digits = (first 6 digits % 89)
     */
    private fun validateLuxembourgCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 8) return false

        val baseNumber = vatBody.substring(0, 6).toIntOrNull() ?: return false
        val checkDigits = vatBody.substring(6).toIntOrNull() ?: return false

        return checkDigits == baseNumber % 89
    }

    /**
     * Polish VAT: Weighted sum mod 11
     */
    private fun validatePolishCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 10) return false

        val weights = listOf(6, 5, 7, 2, 3, 4, 5, 6, 7)
        var sum = 0
        for (i in 0 until 9) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += digit * weights[i]
        }

        val expected = sum % 11
        if (expected == 10) return false // Invalid VAT number

        val checkDigit = vatBody[9].digitToIntOrNull() ?: return false
        return checkDigit == expected
    }

    /**
     * Slovenian VAT: Weighted sum mod 11
     */
    private fun validateSlovenianCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 8) return false

        val weights = listOf(8, 7, 6, 5, 4, 3, 2)
        var sum = 0
        for (i in 0 until 7) {
            val digit = vatBody[i].digitToIntOrNull() ?: return false
            sum += digit * weights[i]
        }

        val remainder = 11 - (sum % 11)
        val expected = when (remainder) {
            10 -> 0
            11 -> return false // Invalid
            else -> remainder
        }

        val checkDigit = vatBody[7].digitToIntOrNull() ?: return false
        return checkDigit == expected
    }

    /**
     * Austrian VAT: Weighted sum with special handling (starts with U)
     */
    private fun validateAustrianCheckDigit(vatBody: String): Boolean {
        if (vatBody.length != 9 || vatBody[0] != 'U') return false

        val digits = vatBody.substring(1)
        val weights = listOf(1, 2, 1, 2, 1, 2, 1)
        var sum = 0

        for (i in 0 until 7) {
            val digit = digits[i].digitToIntOrNull() ?: return false
            val product = digit * weights[i]
            sum += (product / 10) + (product % 10)
        }

        val expected = (10 - ((sum + 4) % 10)) % 10
        val checkDigit = digits[7].digitToIntOrNull() ?: return false
        return checkDigit == expected
    }
}
