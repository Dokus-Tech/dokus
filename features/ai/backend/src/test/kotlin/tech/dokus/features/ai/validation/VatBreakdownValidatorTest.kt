package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.model.VatBreakdownEntry
import kotlin.test.Test
import kotlin.test.assertTrue

class VatBreakdownValidatorTest {

    @Test
    fun `missing vat breakdown required yields warning`() {
        val checks = VatBreakdownValidator.verify(
            vatBreakdown = emptyList(),
            subtotal = Money(10000),
            vatAmount = Money(2100),
            documentDate = LocalDate(2024, 1, 1),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.VAT_BREAKDOWN && !it.passed && it.severity == Severity.WARNING }
        )
    }

    @Test
    fun `valid breakdown passes sum checks`() {
        val breakdown = listOf(
            VatBreakdownEntry(rate = 2100, base = 10000, amount = 2100)
        )
        val checks = VatBreakdownValidator.verify(
            vatBreakdown = breakdown,
            subtotal = Money(10000),
            vatAmount = Money(2100),
            documentDate = LocalDate(2024, 1, 1),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.VAT_BREAKDOWN && it.passed }
        )
    }
}
