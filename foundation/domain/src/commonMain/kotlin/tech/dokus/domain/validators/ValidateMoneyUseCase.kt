package tech.dokus.domain.validators

import tech.dokus.domain.Money

/**
 * Validates monetary amounts.
 *
 * With the new Money representation (minor units as Long),
 * all Money instances are structurally valid by construction.
 * This validator exists for API compatibility.
 */
object ValidateMoneyUseCase : Validator<Money> {

    override operator fun invoke(value: Money): Boolean {
        // Money is always valid by construction (it's just a Long)
        return true
    }
}
