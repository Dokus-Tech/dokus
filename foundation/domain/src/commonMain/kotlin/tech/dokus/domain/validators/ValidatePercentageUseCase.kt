package tech.dokus.domain.validators

import tech.dokus.domain.Percentage

/**
 * Validates percentage values.
 *
 * Requirements:
 * - Must be between 0 and 100% (0-10000 basis points)
 */
object ValidatePercentageUseCase : Validator<Percentage> {

    override operator fun invoke(value: Percentage): Boolean {
        return value.isValid
    }
}
