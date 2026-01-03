@file:Suppress(
    "ReturnCount" // Validation requires multiple early returns
)

package tech.dokus.domain.validators

import tech.dokus.domain.ids.Iban
import kotlin.text.iterator

// IBAN length constraints
private const val IbanMinLength = 15
private const val IbanMaxLength = 34
private const val BelgianIbanLength = 16

// IBAN validation constants
private const val IbanCountryCodeLength = 4
private const val LetterToNumberOffset = 10  // A=10, B=11, etc.
private const val IbanModDivisor = 97
private const val IbanValidRemainder = 1L
private const val DigitBase = 10

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
        if (cleaned.length !in IbanMinLength..IbanMaxLength) {
            return false
        }

        // Belgian IBAN must be exactly 16 characters
        if (cleaned.startsWith("BE") && cleaned.length != BelgianIbanLength) {
            return false
        }

        // Perform mod-97 validation
        return validateIbanCheckDigits(cleaned)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException") // IBAN validation can fail in various ways
    private fun validateIbanCheckDigits(iban: String): Boolean {
        return try {
            // Move first 4 chars to end
            val rearranged = iban.substring(IbanCountryCodeLength) + iban.substring(0, IbanCountryCodeLength)

            // Replace letters with numbers (A=10, B=11, ..., Z=35)
            val numericString = rearranged.map { char ->
                if (char.isDigit()) {
                    char.toString()
                } else {
                    (char.code - 'A'.code + LetterToNumberOffset).toString()
                }
            }.joinToString("")

            // Calculate mod 97 using digit-by-digit approach to handle large numbers
            var remainder = 0L
            for (digit in numericString) {
                remainder = (remainder * DigitBase + digit.digitToInt()) % IbanModDivisor
            }

            // Valid IBAN has mod 97 == 1
            remainder == IbanValidRemainder
        } catch (e: Exception) {
            false
        }
    }
}
