package tech.dokus.features.ai.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.model.FinancialLineItemDto
import tech.dokus.domain.model.VatBreakdownEntryDto
import kotlin.math.abs

@Serializable
data class LineItemToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.LineItemTitle)
    val title: String?,
    @property:LLMDescription(ExtractionToolDescriptions.LineItemDescription)
    val description: String?,
    @property:LLMDescription(ExtractionToolDescriptions.LineItemQuantity)
    val quantity: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.LineItemUnitPrice)
    val unitPrice: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.LineItemVatRate)
    val vatRate: String? = null,
    @property:LLMDescription(ExtractionToolDescriptions.LineItemNetAmount)
    val netAmount: String? = null
)

@Serializable
data class VatBreakdownToolInput(
    @property:LLMDescription(ExtractionToolDescriptions.VatBreakdownRate)
    val rate: String?,
    @property:LLMDescription(ExtractionToolDescriptions.VatBreakdownBase)
    val base: String?,
    @property:LLMDescription(ExtractionToolDescriptions.VatBreakdownAmount)
    val amount: String?
)

internal fun LineItemToolInput.toDomain(): FinancialLineItemDto? {
    val desc = title?.trim().orEmpty()
    if (desc.isEmpty()) return null

    return FinancialLineItemDto(
        description = desc,
        quantity = parseQuantity(quantity),
        unitPrice = Money.from(unitPrice)?.minor,
        vatRate = VatRate.from(vatRate)?.basisPoints,
        netAmount = Money.from(netAmount)?.minor
    )
}

internal fun VatBreakdownToolInput.toDomain(): VatBreakdownEntryDto? {
    val rateValue = VatRate.from(rate)?.basisPoints ?: return null
    val baseValue = Money.from(base)?.minor ?: return null
    val amountValue = Money.from(amount)?.minor ?: return null
    return VatBreakdownEntryDto(
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
