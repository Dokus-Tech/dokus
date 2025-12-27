package ai.dokus.foundation.domain.usecases.validators

import tech.dokus.domain.ids.Iban

/**
 * Validates IBAN (International Bank Account Number)
 *
 * IBAN validation algorithm:
 * 1. Move first 4 characters to the end
 * 2. Replace letters with numbers (A=10, B=11, ..., Z=35)
 * 3. Calculate mod 97
 * 4. Result must be 1
 *
 * Belgian IBAN: Exactly 16 characters (BExx xxxx xxxx xxxx)
 */
object ValidateIbanUseCase : Validator<Iban> {
    override operator fun invoke(value: Iban): Boolean {
        if (value.value.isBlank()) return false

        val cleaned = value.value.replace(" ", "").replace("-", "").uppercase()

        // Check basic format
        if (!cleaned.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))) {
            return false
        }

        // Check length (15-34 chars for all IBANs)
        if (cleaned.length !in 15..34) {
            return false
        }

        // Belgian IBAN must be exactly 16 characters
        if (cleaned.startsWith("BE") && cleaned.length != 16) {
            return false
        }

        // Perform mod-97 validation
        return validateIbanCheckDigits(cleaned)
    }

    private fun validateIbanCheckDigits(iban: String): Boolean {
        return try {
            // Move first 4 chars to end
            val rearranged = iban.substring(4) + iban.substring(0, 4)

            // Replace letters with numbers (A=10, B=11, ..., Z=35)
            val numericString = rearranged.map { char ->
                if (char.isDigit()) {
                    char.toString()
                } else {
                    (char.code - 'A'.code + 10).toString()
                }
            }.joinToString("")

            // Calculate mod 97 using digit-by-digit approach to handle large numbers
            var remainder = 0L
            for (digit in numericString) {
                remainder = (remainder * 10 + digit.digitToInt()) % 97
            }

            // Valid IBAN has mod 97 == 1
            remainder == 1L
        } catch (e: Exception) {
            false
        }
    }
}
