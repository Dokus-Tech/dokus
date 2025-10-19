package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.VatRate

/**
 * Validates VAT rates
 *
 * Requirements:
 * - Format: digits with optional decimal point (1-2 decimal places)
 * - Must be between 0 and 100
 * - Belgian standard rates: 0%, 6%, 12%, 21%
 *
 * Examples: "21.00", "6", "0.00"
 */
object ValidateVatRateUseCase : Validator<VatRate> {
    private val vatRateRegex = Regex("^\\d+(\\.\\d{1,2})?$")

    override operator fun invoke(value: VatRate): Boolean {
        if (!value.value.matches(vatRateRegex)) return false

        val rate = value.value.toDoubleOrNull() ?: return false
        return rate in 0.0..100.0
    }
}
