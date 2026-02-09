package tech.dokus.features.ai.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.VatBreakdownEntry
import kotlin.math.abs

@Serializable
data class LineItemToolInput(
    @property:LLMDescription("Line item description. Required when providing a line item.")
    val description: String?,
    @property:LLMDescription("Quantity as a whole number string (e.g., '2'). Null if not shown.")
    val quantity: String? = null,
    @property:LLMDescription("Unit price (excl VAT) as plain number string (e.g., '12.50'). Null if not shown.")
    val unitPrice: String? = null,
    @property:LLMDescription("VAT rate percentage for this line (e.g., '21'). Null if not shown.")
    val vatRate: String? = null,
    @property:LLMDescription("Line total excl VAT as plain number string. Null if not shown.")
    val netAmount: String? = null
)

@Serializable
data class VatBreakdownToolInput(
    @property:LLMDescription("VAT rate percentage for this row (e.g., '6', '12', '21').")
    val rate: String?,
    @property:LLMDescription("Taxable base (excl VAT) for this rate as plain number string.")
    val base: String?,
    @property:LLMDescription("VAT amount for this rate as plain number string.")
    val amount: String?
)

internal fun LineItemToolInput.toDomain(): FinancialLineItem? {
    val desc = description?.trim().orEmpty()
    if (desc.isEmpty()) return null

    return FinancialLineItem(
        description = desc,
        quantity = parseQuantity(quantity),
        unitPrice = Money.from(unitPrice)?.minor,
        vatRate = VatRate.from(vatRate)?.basisPoints,
        netAmount = Money.from(netAmount)?.minor
    )
}

internal fun VatBreakdownToolInput.toDomain(): VatBreakdownEntry? {
    val rateValue = VatRate.from(rate)?.basisPoints ?: return null
    val baseValue = Money.from(base)?.minor ?: return null
    val amountValue = Money.from(amount)?.minor ?: return null
    return VatBreakdownEntry(
        rate = rateValue,
        base = baseValue,
        amount = amountValue
    )
}

private fun parseQuantity(raw: String?): Long? {
    val cleaned = raw?.trim()?.replace(",", ".") ?: return null
    if (cleaned.isEmpty()) return null
    val numeric = cleaned.toDoubleOrNull() ?: return null
    val whole = numeric.toLong()
    return if (abs(numeric - whole.toDouble()) < 0.000001) whole else null
}
