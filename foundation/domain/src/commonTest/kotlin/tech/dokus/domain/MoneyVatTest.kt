package tech.dokus.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyVatTest {

    @Test
    fun `vat rate applies in minor units`() {
        val amount = Money.fromDouble(10.00) // 1000 minor units
        val vat = VatRate.STANDARD_BE

        assertEquals(Money(210), vat.applyTo(amount))
    }

    @Test
    fun `vat rate truncates fractional cents`() {
        val amount = Money(1) // 0.01
        val vat = VatRate.STANDARD_BE

        assertEquals(Money.ZERO, vat.applyTo(amount))
    }
}
