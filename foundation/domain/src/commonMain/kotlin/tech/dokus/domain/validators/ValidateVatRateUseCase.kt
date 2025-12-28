package tech.dokus.domain.validators

import tech.dokus.domain.VatRate

/**
 * Validates VAT rates.
 *
 * Requirements:
 * - Must be between 0 and 100% (0-10000 basis points)
 * - Belgian standard rates: 0%, 6%, 12%, 21%
 */
object ValidateVatRateUseCase : Validator<VatRate> {

    override operator fun invoke(value: VatRate): Boolean {
        return value.isValid
    }
}
