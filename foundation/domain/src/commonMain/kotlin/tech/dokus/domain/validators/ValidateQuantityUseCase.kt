package tech.dokus.domain.validators

import tech.dokus.domain.Quantity

/**
 * Validates quantity values
 *
 * Requirements:
 * - Format: digits with optional decimal point
 * - Must be positive (greater than 0)
 *
 * Examples: "1", "10.5", "0.25"
 */
object ValidateQuantityUseCase : Validator<Quantity> {
    private val quantityRegex = Regex("^\\d+(\\.\\d+)?$")

    override operator fun invoke(value: Quantity): Boolean {
        if (!value.value.matches(quantityRegex)) return false

        val quantity = value.value.toDoubleOrNull() ?: return false
        return quantity > 0.0
    }
}
