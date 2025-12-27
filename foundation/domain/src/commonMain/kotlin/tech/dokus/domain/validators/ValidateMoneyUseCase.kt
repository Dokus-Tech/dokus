package tech.dokus.domain.validators

import tech.dokus.domain.Money

/**
 * Validates monetary amounts
 *
 * Requirements:
 * - Format: optional minus sign, digits, optional decimal point with 1-2 decimal places
 * - Examples: "123.45", "-50.00", "1000", "0.5"
 */
object ValidateMoneyUseCase : Validator<Money> {
    private val moneyRegex = Regex("^-?\\d+(\\.\\d{1,2})?$")

    override operator fun invoke(value: Money): Boolean {
        return value.value.matches(moneyRegex)
    }
}
