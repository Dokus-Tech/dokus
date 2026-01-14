package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BelgianVatRateValidatorTest {

    // =========================================================================
    // Standard Belgian VAT rates
    // =========================================================================

    @Test
    fun `verify passes for 21 percent standard rate`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("21.00")

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertTrue(result.passed)
        assertEquals(CheckType.VAT_RATE, result.type)
        assertTrue(result.message.contains("21%"))
    }

    @Test
    fun `verify passes for 6 percent reduced rate`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("6.00")

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertTrue(result.passed)
        assertTrue(result.message.contains("6%"))
    }

    @Test
    fun `verify passes for 12 percent intermediate rate`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("12.00")

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertTrue(result.passed)
        assertTrue(result.message.contains("12%"))
    }

    @Test
    fun `verify passes for 0 percent exempt rate`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("0.00")

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertTrue(result.passed)
        assertTrue(result.message.contains("0%"))
    }

    // =========================================================================
    // Rate tolerance
    // =========================================================================

    @Test
    fun `verify passes within 0_5 percent tolerance`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("20.50") // ~20.5%, within 0.5% of 21%

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertTrue(result.passed)
    }

    @Test
    fun `verify warns for unusual rate outside tolerance`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("15.00") // 15% - not a Belgian rate

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, null)

        assertFalse(result.passed)
        assertEquals(Severity.WARNING, result.severity)
        assertTrue(result.message.contains("Unusual"))
        assertTrue(result.hint?.contains("Belgian") == true)
    }

    // =========================================================================
    // March 2026 VAT reform - Horeca
    // =========================================================================

    @Test
    fun `verify 12 percent for restaurant category post March 2026`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("12.00")
        val postReformDate = LocalDate(2026, 4, 1) // After March 2026

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, postReformDate, "RESTAURANT")

        assertTrue(result.passed)
        assertTrue(result.message.contains("post-March 2026"))
        assertTrue(result.message.contains("beverages"))
    }

    @Test
    fun `verify 12 percent for hotel category post March 2026`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("12.00")
        val postReformDate = LocalDate(2026, 5, 15)

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, postReformDate, "Hotel accommodation")

        assertTrue(result.passed)
        assertTrue(result.message.contains("post-March 2026"))
    }

    @Test
    fun `verify 12 percent for cafe pre March 2026 notes food only`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("12.00")
        val preReformDate = LocalDate(2025, 12, 1) // Before March 2026

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, preReformDate, "CAFE")

        assertTrue(result.passed)
        assertTrue(result.message.contains("pre-March 2026"))
        assertTrue(result.message.contains("food only"))
    }

    @Test
    fun `verify 12 percent for non-Horeca category`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("12.00")

        val result = BelgianVatRateValidator.verify(subtotal, vatAmount, null, "CONSTRUCTION")

        assertTrue(result.passed)
        assertTrue(result.message.contains("intermediate rate"))
    }

    // =========================================================================
    // Edge cases
    // =========================================================================

    @Test
    fun `verify returns incomplete when subtotal is null`() {
        val result = BelgianVatRateValidator.verify(null, Money.parse("21.00"), null, null)

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertTrue(result.message.contains("Cannot verify"))
    }

    @Test
    fun `verify returns incomplete when subtotal is zero`() {
        val result = BelgianVatRateValidator.verify(Money.parse("0.00"), Money.parse("0.00"), null, null)

        assertTrue(result.passed)
        assertTrue(result.message.contains("zero"))
    }

    // =========================================================================
    // Helper functions
    // =========================================================================

    @Test
    fun `isHorecaCategory returns true for restaurant`() {
        assertTrue(BelgianVatRateValidator.isHorecaCategory("RESTAURANT"))
        assertTrue(BelgianVatRateValidator.isHorecaCategory("restaurant food"))
        assertTrue(BelgianVatRateValidator.isHorecaCategory("Cafe order"))
        assertTrue(BelgianVatRateValidator.isHorecaCategory("HOTEL stay"))
    }

    @Test
    fun `isHorecaCategory returns false for non-Horeca`() {
        assertFalse(BelgianVatRateValidator.isHorecaCategory("OFFICE_SUPPLIES"))
        assertFalse(BelgianVatRateValidator.isHorecaCategory("Construction"))
        assertFalse(BelgianVatRateValidator.isHorecaCategory(null))
    }

    @Test
    fun `isPostReform returns correct values`() {
        assertFalse(BelgianVatRateValidator.isPostReform(null))
        assertFalse(BelgianVatRateValidator.isPostReform(LocalDate(2026, 2, 28)))
        assertTrue(BelgianVatRateValidator.isPostReform(LocalDate(2026, 3, 1)))
        assertTrue(BelgianVatRateValidator.isPostReform(LocalDate(2027, 1, 1)))
    }
}
