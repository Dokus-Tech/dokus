package tech.dokus.domain.validators

import tech.dokus.domain.Percentage

/**
 * Validates percentage values
 *
 * Requirements:
 * - Format: digits with optional decimal point (1-2 decimal places)
 * - Must be between 0 and 100
 *
 * Examples: "100.00", "50", "33.33"
 */
object ValidatePercentageUseCase : Validator<Percentage> {
    private val percentageRegex = Regex("^\\d+(\\.\\d{1,2})?$")

    override operator fun invoke(value: Percentage): Boolean {
        if (!value.value.matches(percentageRegex)) return false

        val percent = value.value.toDoubleOrNull() ?: return false
        return percent in 0.0..100.0
    }
}
