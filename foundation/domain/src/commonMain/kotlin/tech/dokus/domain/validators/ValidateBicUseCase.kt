package tech.dokus.domain.validators

import tech.dokus.domain.ids.Bic

/**
 * Validates BIC/SWIFT codes
 *
 * BIC format: XXXXYYZZQQQ or XXXXYYZZ
 * - XXXX: 4 letter bank code
 * - YY: 2 letter country code
 * - ZZ: 2 character location code
 * - QQQ: 3 character branch code (optional)
 *
 * Examples:
 * - GEBABEBB (8 characters)
 * - GEBABEBB036 (11 characters)
 */
object ValidateBicUseCase : Validator<Bic> {
    /** BIC length without branch code */
    private const val BicLengthShort = 8

    /** BIC length with branch code */
    private const val BicLengthFull = 11

    override operator fun invoke(value: Bic): Boolean {
        if (value.value.isBlank()) return false

        val cleaned = value.value.replace(" ", "").uppercase()

        // Must be exactly 8 or 11 characters
        if (cleaned.length != BicLengthShort && cleaned.length != BicLengthFull) {
            return false
        }

        // Validate format: 6 letters followed by 2 alphanumeric, optionally followed by 3 alphanumeric
        return cleaned.matches(Regex("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$"))
    }
}
