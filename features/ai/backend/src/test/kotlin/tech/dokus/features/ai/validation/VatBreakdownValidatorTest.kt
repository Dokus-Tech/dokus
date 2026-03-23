package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.model.VatBreakdownEntryDto
import kotlin.test.Test
import kotlin.test.assertTrue

class VatBreakdownValidatorTest {

    @Test
    fun `missing vat breakdown required yields warning`() {
        val checks = VatBreakdownValidator.verify(
            vatBreakdown = emptyList(),
            subtotal = Money(10000, Currency.Eur),
            vatAmount = Money(2100, Currency.Eur),
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
            VatBreakdownEntryDto(rate = 2100, base = 10000, amount = 2100)
        )
        val checks = VatBreakdownValidator.verify(
            vatBreakdown = breakdown,
            subtotal = Money(10000, Currency.Eur),
            vatAmount = Money(2100, Currency.Eur),
            documentDate = LocalDate(2024, 1, 1),
            required = true
        )

        assertTrue(
            checks.any { it.type == CheckType.VAT_BREAKDOWN && it.passed }
        )
    }
}
