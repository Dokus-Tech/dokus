package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.VatNumber

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
        val digits = cleaned.substring(2)
        val baseNumber = digits.substring(0, 8).toIntOrNull() ?: return false
        val checkDigits = digits.substring(8).toIntOrNull() ?: return false
        val expectedCheckDigits = 97 - (baseNumber % 97)

        return checkDigits == expectedCheckDigits
    }

    private fun validateGenericVat(cleaned: String): Boolean {
        // Generic validation: 2 letter country code followed by alphanumeric
        return cleaned.matches(Regex("^[A-Z]{2}[A-Z0-9]+$"))
    }
}
