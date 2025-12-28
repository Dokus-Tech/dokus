package tech.dokus.domain.validators

import tech.dokus.domain.Quantity

/**
 * Validates quantity values.
 *
 * Requirements:
 * - Must be positive (greater than 0)
 */
object ValidateQuantityUseCase : Validator<Quantity> {

    override operator fun invoke(value: Quantity): Boolean {
        return value.isPositive
    }
}
