package tech.dokus.features.ai.validation

import tech.dokus.domain.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathValidatorTest {

    // =========================================================================
    // verifyTotals tests
    // =========================================================================

    @Test
    fun `verifyTotals passes when math is correct`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("21.00")
        val total = Money.parse("121.00")

        val result = MathValidator.verifyTotals(subtotal, vatAmount, total)

        assertTrue(result.passed)
        assertEquals(CheckType.MATH, result.type)
        assertEquals(Severity.INFO, result.severity)
    }

    @Test
    fun `verifyTotals passes within 2 cent tolerance`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("21.00")
        val total = Money.parse("121.02") // Off by 2 cents

        val result = MathValidator.verifyTotals(subtotal, vatAmount, total)

        assertTrue(result.passed)
    }

    @Test
    fun `verifyTotals fails when difference exceeds tolerance`() {
        val subtotal = Money.parse("100.00")
        val vatAmount = Money.parse("21.00")
        val total = Money.parse("120.00") // Off by 1.00

        val result = MathValidator.verifyTotals(subtotal, vatAmount, total)

        assertFalse(result.passed)
        assertEquals(Severity.CRITICAL, result.severity)
        assertEquals("121.00", result.expected)
        assertEquals("120.00", result.actual)
        assertTrue(result.hint?.contains("Re-read") == true)
    }

    @Test
    fun `verifyTotals returns incomplete when subtotal is null`() {
        val vatAmount = Money.parse("21.00")
        val total = Money.parse("121.00")

        val result = MathValidator.verifyTotals(null, vatAmount, total)

        assertTrue(result.passed) // Incomplete is not a failure
        assertEquals(Severity.INFO, result.severity)
        assertTrue(result.message.contains("missing"))
    }

    @Test
    fun `verifyTotals returns incomplete when all amounts null`() {
        val result = MathValidator.verifyTotals(null, null, null)

        assertTrue(result.passed)
        assertTrue(result.message.contains("No amounts"))
    }

    // =========================================================================
    // verifyLineItems tests
    // =========================================================================

    @Test
    fun `verifyLineItems passes when sum matches subtotal`() {
        val lineItemTotals = listOf(
            Money.parse("50.00")!!,
            Money.parse("30.00")!!,
            Money.parse("20.00")!!
        )
        val subtotal = Money.parse("100.00")

        val result = MathValidator.verifyLineItems(lineItemTotals, subtotal)

        assertTrue(result.passed)
        assertEquals(CheckType.MATH, result.type)
    }

    @Test
    fun `verifyLineItems warns when sum does not match`() {
        val lineItemTotals = listOf(
            Money.parse("50.00")!!,
            Money.parse("30.00")!!
        )
        val subtotal = Money.parse("100.00")

        val result = MathValidator.verifyLineItems(lineItemTotals, subtotal)

        assertFalse(result.passed)
        assertEquals(Severity.WARNING, result.severity) // Not CRITICAL
        assertTrue(result.hint?.contains("missing line items") == true)
    }

    @Test
    fun `verifyLineItems returns incomplete when no line items`() {
        val result = MathValidator.verifyLineItems(emptyList(), Money.parse("100.00"))

        assertTrue(result.passed)
        assertTrue(result.message.contains("No line items"))
    }

    // =========================================================================
    // verifyLineItemCalculation tests
    // =========================================================================

    @Test
    fun `verifyLineItemCalculation passes when quantity times unit price equals total`() {
        val quantity = 3.0
        val unitPrice = Money.parse("25.00")
        val lineTotal = Money.parse("75.00")

        val result = MathValidator.verifyLineItemCalculation(quantity, unitPrice, lineTotal, 1)

        assertTrue(result.passed)
        assertEquals("lineItem[1]", result.field)
    }

    @Test
    fun `verifyLineItemCalculation warns when calculation is wrong`() {
        val quantity = 3.0
        val unitPrice = Money.parse("25.00")
        val lineTotal = Money.parse("100.00") // Should be 75.00

        val result = MathValidator.verifyLineItemCalculation(quantity, unitPrice, lineTotal, 2)

        assertFalse(result.passed)
        assertEquals(Severity.WARNING, result.severity)
        assertEquals("lineItem[2]", result.field)
    }

    @Test
    fun `verifyLineItemCalculation returns incomplete when values missing`() {
        val result = MathValidator.verifyLineItemCalculation(null, null, null, 1)

        assertTrue(result.passed)
        assertTrue(result.message.contains("missing"))
    }
}
