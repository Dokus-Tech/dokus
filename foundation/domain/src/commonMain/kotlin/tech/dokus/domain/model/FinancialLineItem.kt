package tech.dokus.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FinancialLineItemDto(
    val description: String,
    val quantity: Long? = null,
    val unitPrice: Long? = null,
    val vatRate: Int? = null,
    val netAmount: Long? = null
) {
    companion object
}

@Serializable
data class VatBreakdownEntryDto(
    val rate: Int,
    val base: Long,
    val amount: Long
)
