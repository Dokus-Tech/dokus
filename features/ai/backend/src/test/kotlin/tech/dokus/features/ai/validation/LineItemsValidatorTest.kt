package tech.dokus.features.ai.validation

import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.model.FinancialLineItemDto
import kotlin.test.Test
import kotlin.test.assertTrue

class LineItemsValidatorTest {

    @Test
    fun `missing line items required yields warning`() {
        val checks = LineItemsValidator.verify(
            lineItems = emptyList(),
            subtotal = Money(10000, Currency.Eur),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.LINE_ITEMS && !it.passed && it.severity == Severity.WARNING }
        )
    }

    @Test
    fun `line items sum matches subtotal passes`() {
        val items = listOf(
            FinancialLineItemDto(
                description = "Service",
                netAmount = 10000
            )
        )
        val checks = LineItemsValidator.verify(
            lineItems = items,
            subtotal = Money(10000, Currency.Eur),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.MATH && it.passed }
        )
    }

    @Test
    fun `line items missing net amounts yields warning`() {
        val items = listOf(
            FinancialLineItemDto(description = "Service")
        )
        val checks = LineItemsValidator.verify(
            lineItems = items,
            subtotal = Money(10000, Currency.Eur),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.LINE_ITEMS && !it.passed }
        )
    }
}
