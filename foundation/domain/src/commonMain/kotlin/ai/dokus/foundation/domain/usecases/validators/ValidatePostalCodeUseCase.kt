package ai.dokus.foundation.domain.usecases.validators

import tech.dokus.domain.ids.PostalCode

/**
 * Validates Belgian postal codes
 *
 * Belgian postal code validation:
 * - Must be exactly 4 digits
 * - Range must be 1000-9999
 * - Leading zeros are not valid (e.g., 0999 is invalid)
 */
object ValidatePostalCodeUseCase : Validator<PostalCode> {
    override operator fun invoke(value: PostalCode): Boolean {
        if (value.value.isBlank()) return false

        val cleaned = value.value.trim()

        // Must be exactly 4 digits
        if (!cleaned.matches(Regex("^\\d{4}$"))) {
            return false
        }

        // Convert to number and check range 1000-9999
        val numericValue = cleaned.toIntOrNull() ?: return false
        return numericValue in 1000..9999
    }
}
