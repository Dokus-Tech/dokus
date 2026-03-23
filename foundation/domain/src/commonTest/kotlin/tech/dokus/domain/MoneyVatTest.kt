package tech.dokus.domain

import tech.dokus.domain.enums.Currency
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyVatTest {

    @Test
    fun `vat rate applies in minor units`() {
        val amount = Money.fromDouble(10.00, Currency.Eur) // 1000 minor units
        val vat = VatRate.STANDARD_BE

        assertEquals(Money(210, Currency.Eur), vat.applyTo(amount))
    }

    @Test
    fun `vat rate truncates fractional cents`() {
        val amount = Money(1, Currency.Eur) // 0.01
        val vat = VatRate.STANDARD_BE

        assertEquals(Money.zero(Currency.Eur), vat.applyTo(amount))
    }
}
