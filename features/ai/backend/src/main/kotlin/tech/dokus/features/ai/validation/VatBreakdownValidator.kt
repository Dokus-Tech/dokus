package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.model.VatBreakdownEntry
import kotlin.math.abs

object VatBreakdownValidator {

    private const val TOLERANCE_CENTS = 2L
    private const val RATE_TOLERANCE_BP = 50

    fun verify(
        vatBreakdown: List<VatBreakdownEntry>,
        subtotal: Money?,
        vatAmount: Money?,
        documentDate: LocalDate?,
        required: Boolean
    ): List<AuditCheck> = buildList {
        if (vatBreakdown.isEmpty()) {
            add(missingBreakdown(required, "No VAT breakdown extracted"))
            return@buildList
        }

        val baseSum = Money(vatBreakdown.sumOf { it.base })
        val amountSum = Money(vatBreakdown.sumOf { it.amount })

        if (subtotal == null) {
            add(missingBreakdown(required, "Subtotal missing, cannot verify VAT breakdown base sum"))
        } else {
            add(verifySum("vatBreakdown.base", baseSum, subtotal, "VAT base sum"))
        }

        if (vatAmount == null) {
            add(missingBreakdown(required, "VAT amount missing, cannot verify VAT breakdown sum"))
        } else {
            add(verifySum("vatBreakdown.amount", amountSum, vatAmount, "VAT amount sum"))
        }

        vatBreakdown.forEachIndexed { index, entry ->
            val impliedRateCheck = BelgianVatRateValidator
                .verify(Money(entry.base), Money(entry.amount), documentDate, null)
                .copy(field = "vatBreakdown[$index].impliedRate")
            add(impliedRateCheck)

            val rateMatchCheck = verifyRateMatch(entry, index)
            add(rateMatchCheck)
        }
    }

    private fun missingBreakdown(required: Boolean, message: String): AuditCheck {
        return if (required) {
            AuditCheck.warning(
                type = CheckType.VAT_BREAKDOWN,
                field = "vatBreakdown",
                message = message
            )
        } else {
            AuditCheck.incomplete(
                type = CheckType.VAT_BREAKDOWN,
                field = "vatBreakdown",
                message = message
            )
        }
    }

    private fun verifySum(field: String, actual: Money, expected: Money, label: String): AuditCheck {
        val difference = abs(actual.minor - expected.minor)
        return if (difference <= TOLERANCE_CENTS) {
            AuditCheck.passed(
                type = CheckType.VAT_BREAKDOWN,
                field = field,
                message = "$label verified: ${actual.toDisplayString()}"
            )
        } else {
            AuditCheck.warning(
                type = CheckType.VAT_BREAKDOWN,
                field = field,
                message = "$label mismatch: ${actual.toDisplayString()} != ${expected.toDisplayString()}",
                expected = expected.toDisplayString(),
                actual = actual.toDisplayString()
            )
        }
    }

    private fun verifyRateMatch(entry: VatBreakdownEntry, index: Int): AuditCheck {
        if (entry.base == 0L) {
            return AuditCheck.incomplete(
                type = CheckType.VAT_BREAKDOWN,
                field = "vatBreakdown[$index].rate",
                message = "VAT base is zero, cannot verify rate"
            )
        }

        val impliedRate = ((entry.amount * 10000) / entry.base).toInt()
        val deviation = abs(impliedRate - entry.rate)

        return if (deviation <= RATE_TOLERANCE_BP) {
            AuditCheck.passed(
                type = CheckType.VAT_BREAKDOWN,
                field = "vatBreakdown[$index].rate",
                message = "VAT rate verified: ${formatRate(entry.rate)}"
            )
        } else {
            AuditCheck.warning(
                type = CheckType.VAT_BREAKDOWN,
                field = "vatBreakdown[$index].rate",
                message = "VAT rate mismatch: ${formatRate(entry.rate)} vs ~${formatRate(impliedRate)}",
                expected = formatRate(impliedRate),
                actual = formatRate(entry.rate)
            )
        }
    }

    private fun formatRate(basisPoints: Int): String {
        val percent = basisPoints / 100
        val decimal = basisPoints % 100
        return if (decimal == 0) {
            "$percent%"
        } else {
            "$percent.${decimal.toString().padStart(2, '0')}%"
        }
    }
}
