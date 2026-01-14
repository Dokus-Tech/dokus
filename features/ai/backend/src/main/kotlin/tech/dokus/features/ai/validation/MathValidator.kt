package tech.dokus.features.ai.validation

import tech.dokus.domain.Money
import kotlin.math.abs

/**
 * Validates mathematical correctness of extracted financial data.
 *
 * Key validations:
 * - Subtotal + VAT = Total (with rounding tolerance)
 * - Sum of line items = Subtotal
 * - Implied VAT rate matches standard Belgian rates
 */
object MathValidator {

    /**
     * Tolerance for rounding errors: +/- 2 cents (0.02 EUR)
     * Multi-line invoices can accumulate small rounding differences.
     */
    private const val TOLERANCE_CENTS = 2L

    /**
     * Verifies that subtotal + VAT amount equals total amount.
     *
     * @param subtotal The net amount before VAT (nullable)
     * @param vatAmount The VAT amount (nullable)
     * @param total The total amount including VAT (nullable)
     * @return AuditCheck with result and hint for self-correction
     */
    fun verifyTotals(
        subtotal: Money?,
        vatAmount: Money?,
        total: Money?
    ): AuditCheck {
        // If all amounts are missing, can't verify
        if (subtotal == null && vatAmount == null && total == null) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "totals",
                message = "No amounts to verify"
            )
        }

        // If only total is present, can't verify math
        if (subtotal == null || vatAmount == null) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "totals",
                message = "Subtotal or VAT amount missing, cannot verify math"
            )
        }

        // If total is missing, can't compare
        if (total == null) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "totals",
                message = "Total amount missing, cannot verify math"
            )
        }

        // Calculate expected total
        val expected = subtotal + vatAmount
        val difference = abs(expected.minor - total.minor)

        return if (difference <= TOLERANCE_CENTS) {
            AuditCheck.passed(
                type = CheckType.MATH,
                field = "totals",
                message = "Math verified: ${subtotal.toDisplayString()} + ${vatAmount.toDisplayString()} = ${total.toDisplayString()}"
            )
        } else {
            AuditCheck.criticalFailure(
                type = CheckType.MATH,
                field = "totals",
                message = "Math error: ${subtotal.toDisplayString()} + ${vatAmount.toDisplayString()} != ${total.toDisplayString()}",
                hint = buildMathHint(expected, total),
                expected = expected.toDisplayString(),
                actual = total.toDisplayString()
            )
        }
    }

    /**
     * Verifies that the sum of line item totals equals the subtotal.
     *
     * @param lineItemTotals List of line item total amounts
     * @param subtotal The expected subtotal
     * @return AuditCheck with result
     */
    fun verifyLineItems(
        lineItemTotals: List<Money>,
        subtotal: Money?
    ): AuditCheck {
        if (lineItemTotals.isEmpty()) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "lineItems",
                message = "No line items to verify"
            )
        }

        if (subtotal == null) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "lineItems",
                message = "Subtotal missing, cannot verify line items sum"
            )
        }

        val sum = lineItemTotals.fold(Money.ZERO) { acc, item -> acc + item }
        val difference = abs(sum.minor - subtotal.minor)

        return if (difference <= TOLERANCE_CENTS) {
            AuditCheck.passed(
                type = CheckType.MATH,
                field = "lineItems",
                message = "Line items sum verified: ${sum.toDisplayString()} matches subtotal"
            )
        } else {
            // This is a WARNING, not CRITICAL - could be hidden items
            AuditCheck.warning(
                type = CheckType.MATH,
                field = "lineItems",
                message = "Line items sum (${sum.toDisplayString()}) doesn't match subtotal (${subtotal.toDisplayString()})",
                hint = "Check for missing line items, discounts, or rounding differences",
                expected = subtotal.toDisplayString(),
                actual = sum.toDisplayString()
            )
        }
    }

    /**
     * Verifies that a single line item's quantity * unit price = total.
     *
     * @param quantity The quantity (as string for parsing)
     * @param unitPrice The unit price
     * @param lineTotal The expected line total
     * @param lineIndex Line index for error messages (1-based)
     * @return AuditCheck with result
     */
    fun verifyLineItemCalculation(
        quantity: Double?,
        unitPrice: Money?,
        lineTotal: Money?,
        lineIndex: Int
    ): AuditCheck {
        if (quantity == null || unitPrice == null || lineTotal == null) {
            return AuditCheck.incomplete(
                type = CheckType.MATH,
                field = "lineItem[$lineIndex]",
                message = "Line item $lineIndex: missing quantity, unit price, or total"
            )
        }

        // Calculate expected: quantity * unitPrice
        // Using minor units: (quantity * unitPrice.minor).toLong()
        val expectedMinor = (quantity * unitPrice.minor).toLong()
        val expected = Money(expectedMinor)
        val difference = abs(expected.minor - lineTotal.minor)

        return if (difference <= TOLERANCE_CENTS) {
            AuditCheck.passed(
                type = CheckType.MATH,
                field = "lineItem[$lineIndex]",
                message = "Line $lineIndex math verified"
            )
        } else {
            AuditCheck.warning(
                type = CheckType.MATH,
                field = "lineItem[$lineIndex]",
                message = "Line $lineIndex: $quantity x ${unitPrice.toDisplayString()} != ${lineTotal.toDisplayString()}",
                hint = "Check quantity and unit price for line item $lineIndex",
                expected = expected.toDisplayString(),
                actual = lineTotal.toDisplayString()
            )
        }
    }

    /**
     * Builds a helpful hint for math errors.
     */
    private fun buildMathHint(expected: Money, actual: Money): String {
        val diffCents = abs(expected.minor - actual.minor)

        return buildString {
            append("Re-read the total amount. ")
            append("Expected ${expected.toDisplayString()}, found ${actual.toDisplayString()}. ")

            // Provide specific guidance based on the difference
            when {
                diffCents == 1L -> append("Off by 1 cent - likely a rounding issue. ")
                diffCents in 2..10 -> append("Small difference - check for rounding. ")
                diffCents in 10..100 -> append("Check for misread digits (e.g., 1<->7, 0<->6). ")
                diffCents in 100..1000 -> append("Check decimal point position. ")
                else -> append("Significant difference - verify all amounts. ")
            }

            append("Common causes: misread digits, wrong decimal position, missed negative sign.")
        }
    }
}
