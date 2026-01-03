package tech.dokus.domain.validators

import tech.dokus.domain.ids.VatNumber

/**
 * Validates VAT numbers with Belgian-specific validation
 *
 * Belgian VAT validation algorithm:
 * - Format: BE + 10 digits (e.g., BE0123456789)
 * - Check digits: last 2 digits must equal 97 - (first 8 digits % 97)
 *
 * For other countries, performs basic format validation.
 */
object ValidateVatNumberUseCase : Validator<VatNumber> {
    /** Modulo divisor for Belgian VAT check digit algorithm */
    private const val Mod97Divisor = 97

    /** Start index of check digits in Belgian VAT number */
    private const val CheckDigitsStart = 8

    /** Country code prefix length */
    private const val CountryCodeLength = 2

    override operator fun invoke(value: VatNumber): Boolean {
        if (value.value.isBlank()) return false

        val cleaned = value.value.replace(".", "").replace(" ", "").uppercase()

        // Belgian VAT number validation
        return if (cleaned.startsWith("BE")) {
            validateBelgianVat(cleaned)
        } else {
            validateGenericVat(cleaned)
        }
    }

    private fun validateBelgianVat(cleaned: String): Boolean {
        // Must be BE followed by exactly 10 digits
        if (!cleaned.matches(Regex("^BE[0-9]{10}$"))) {
            return false
        }

        // Validate check digits using modulo-97 algorithm
        val digits = cleaned.substring(CountryCodeLength)
        val baseNumber = digits.substring(0, CheckDigitsStart).toIntOrNull() ?: return false
        val checkDigits = digits.substring(CheckDigitsStart).toIntOrNull() ?: return false
        val expectedCheckDigits = Mod97Divisor - (baseNumber % Mod97Divisor)

        return checkDigits == expectedCheckDigits
    }

    private fun validateGenericVat(cleaned: String): Boolean {
        // Generic validation: 2 letter country code followed by alphanumeric
        return cleaned.matches(Regex("^[A-Z]{2}[A-Z0-9]+$"))
    }
}
