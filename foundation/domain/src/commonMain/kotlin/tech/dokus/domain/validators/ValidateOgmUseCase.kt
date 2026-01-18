package tech.dokus.domain.validators

/**
 * Belgian Structured Communication (Gestructureerde Mededeling / Communication Structuree)
 *
 * Format: +++XXX/XXXX/XXXXX+++ or ***XXX/XXXX/XXXXX***
 * Total: 12 digits in 3/4/5 grouping
 *
 * Check digit algorithm:
 * - Take first 10 digits as the base number
 * - Last 2 digits = base % 97
 * - Exception: if result is 0, use 97 instead
 *
 * Common OCR misreads to detect and correct:
 * - 0 <-> O (zero vs letter O)
 * - 1 <-> I <-> l (one vs I vs lowercase L)
 * - 8 <-> B
 * - 5 <-> S
 * - 6 <-> G
 *
 * Reference: Belgian banking standard (CODA format)
 */
object ValidateOgmUseCase {

    // Strict pattern: only digits
    private val OGM_STRICT = Regex("""[+*]{3}(\d{3})/(\d{4})/(\d{5})[+*]{3}""")

    // Relaxed pattern: allows common OCR mistakes
    private val OGM_RELAXED = Regex("""[+*]{3}([0-9OoIlBbSsGg]{3})/([0-9OoIlBbSsGg]{4})/([0-9OoIlBbSsGg]{5})[+*]{3}""")

    // Alternative formats without delimiters
    private val OGM_NO_DELIMITERS = Regex("""[+*]{3}(\d{12})[+*]{3}""")

    /**
     * Validates an OGM string and returns the result.
     *
     * @param input The OGM string to validate (e.g., "+++123/4567/89012+++")
     * @return Validation result with normalized form if valid
     */
    fun validate(input: String): OgmValidationResult {
        val cleaned = input.trim()

        if (cleaned.isBlank()) {
            return OgmValidationResult.InvalidFormat(
                message = "Empty input",
                hint = "Expected format: +++XXX/XXXX/XXXXX+++"
            )
        }

        // Try strict match first (all digits, correct format)
        val strictMatch = OGM_STRICT.matchEntire(cleaned)
        if (strictMatch != null) {
            val digits = strictMatch.groupValues.drop(1).joinToString("")
            return validateChecksum(digits, cleaned)
        }

        // Try format without slashes
        val noDelimMatch = OGM_NO_DELIMITERS.matchEntire(cleaned)
        if (noDelimMatch != null) {
            val digits = noDelimMatch.groupValues[1]
            return validateChecksum(digits, cleaned)
        }

        // Try relaxed match with OCR corrections
        val relaxedMatch = OGM_RELAXED.matchEntire(cleaned)
        if (relaxedMatch != null) {
            val rawDigits = relaxedMatch.groupValues.drop(1).joinToString("")
            val correctedDigits = correctOcrMistakes(rawDigits)

            val result = validateChecksum(correctedDigits, cleaned)
            return when (result) {
                is OgmValidationResult.Valid -> OgmValidationResult.CorrectedValid(
                    normalized = result.normalized,
                    original = cleaned,
                    corrections = describeCorrections(rawDigits, correctedDigits)
                )
                else -> result
            }
        }

        // Check if it looks like an OGM but has format issues
        if (cleaned.contains("+++") || cleaned.contains("***")) {
            return OgmValidationResult.InvalidFormat(
                message = "Invalid OGM format",
                hint = "Expected format: +++XXX/XXXX/XXXXX+++ with 12 digits"
            )
        }

        return OgmValidationResult.InvalidFormat(
            message = "Not recognized as OGM",
            hint = "OGM format is +++XXX/XXXX/XXXXX+++ or ***XXX/XXXX/XXXXX***"
        )
    }

    /**
     * Validates the Mod-97 checksum of 12 digits.
     */
    private fun validateChecksum(digits: String, original: String): OgmValidationResult {
        if (digits.length != 12) {
            return OgmValidationResult.InvalidFormat(
                message = "OGM must have exactly 12 digits, found ${digits.length}",
                hint = "Format: +++XXX/XXXX/XXXXX+++"
            )
        }

        if (!digits.all { it.isDigit() }) {
            return OgmValidationResult.InvalidFormat(
                message = "OGM contains non-digit characters after correction",
                hint = "All 12 positions must be digits 0-9"
            )
        }

        val base = digits.take(10).toLongOrNull()
        val checkDigits = digits.takeLast(2).toIntOrNull()

        if (base == null || checkDigits == null) {
            return OgmValidationResult.InvalidFormat(
                message = "Could not parse OGM digits",
                hint = "Ensure all characters are digits"
            )
        }

        val expectedCheck = calculateCheckDigits(base)

        return if (checkDigits == expectedCheck) {
            OgmValidationResult.Valid(
                normalized = formatOgm(digits)
            )
        } else {
            OgmValidationResult.InvalidChecksum(
                expected = expectedCheck,
                actual = checkDigits,
                hint = buildChecksumHint(original)
            )
        }
    }

    /**
     * Calculates the 2-digit checksum for a 10-digit base.
     * Formula: base % 97, but if result is 0, use 97
     */
    private fun calculateCheckDigits(base: Long): Int {
        val remainder = (base % 97).toInt()
        return if (remainder == 0) 97 else remainder
    }

    /**
     * Formats 12 digits into standard OGM format.
     */
    private fun formatOgm(digits: String): String {
        require(digits.length == 12) { "OGM must have 12 digits" }
        return "+++${digits.substring(0, 3)}/${digits.substring(3, 7)}/${digits.substring(7)}+++"
    }

    /**
     * Corrects common OCR mistakes in OGM strings.
     */
    private fun correctOcrMistakes(input: String): String = input
        .replace('O', '0').replace('o', '0')  // Letter O -> zero
        .replace('I', '1').replace('l', '1')  // Letter I/l -> one
        .replace('B', '8').replace('b', '8')  // Letter B -> eight
        .replace('S', '5').replace('s', '5')  // Letter S -> five
        .replace('G', '6').replace('g', '6')  // Letter G -> six

    /**
     * Describes what OCR corrections were applied.
     */
    private fun describeCorrections(original: String, corrected: String): String {
        val corrections = mutableListOf<String>()
        for (i in original.indices) {
            if (original[i] != corrected[i]) {
                corrections.add("'${original[i]}' -> '${corrected[i]}'")
            }
        }
        return if (corrections.isEmpty()) {
            "No corrections needed"
        } else {
            "Applied OCR corrections: ${corrections.joinToString(", ")}"
        }
    }

    /**
     * Builds a helpful hint for checksum failures.
     */
    private fun buildChecksumHint(original: String): String = buildString {
        append("Checksum verification failed. ")
        append("Common OCR mistakes to check: ")
        append("0 <-> O (zero/letter O), ")
        append("1 <-> I/l (one/letter I/lowercase L), ")
        append("8 <-> B, 5 <-> S, 6 <-> G. ")
        append("Please re-read the payment reference carefully.")
    }

    /**
     * Generates a valid OGM from a 10-digit base number.
     *
     * @param base10Digits The base number (must be 0-9,999,999,999)
     * @return Formatted OGM string with valid checksum
     */
    fun generate(base10Digits: Long): String {
        require(base10Digits in 0..9_999_999_999L) {
            "Base must be 10 digits (0 to 9,999,999,999)"
        }
        val checkDigits = calculateCheckDigits(base10Digits)
        val basePadded = base10Digits.toString().padStart(10, '0')
        val checkPadded = checkDigits.toString().padStart(2, '0')
        return formatOgm(basePadded + checkPadded)
    }

    /**
     * Checks if a string looks like it might be an OGM (for quick filtering).
     */
    fun looksLikeOgm(input: String): Boolean {
        val trimmed = input.trim()
        return trimmed.startsWith("+++") || trimmed.startsWith("***")
    }
}

/**
 * Result of OGM validation.
 */
sealed class OgmValidationResult {

    /**
     * The OGM is valid with correct checksum.
     */
    data class Valid(
        val normalized: String
    ) : OgmValidationResult()

    /**
     * The OGM was valid after applying OCR corrections.
     */
    data class CorrectedValid(
        val normalized: String,
        val original: String,
        val corrections: String
    ) : OgmValidationResult()

    /**
     * The OGM format is invalid.
     */
    data class InvalidFormat(
        val message: String,
        val hint: String
    ) : OgmValidationResult()

    /**
     * The OGM format is correct but checksum is invalid.
     */
    data class InvalidChecksum(
        val expected: Int,
        val actual: Int,
        val hint: String
    ) : OgmValidationResult()

    /**
     * Returns true if the OGM is valid (including corrected).
     */
    val isValid: Boolean
        get() = this is Valid || this is CorrectedValid

    /**
     * Returns the normalized OGM if valid, null otherwise.
     */
    val normalizedOrNull: String?
        get() = when (this) {
            is Valid -> normalized
            is CorrectedValid -> normalized
            else -> null
        }
}
