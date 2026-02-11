package tech.dokus.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class FinancialLineItem(
    val description: String,
    val quantity: Long? = null,
    val unitPrice: Long? = null,
    val vatRate: Int? = null,
    val netAmount: Long? = null
)

@Serializable
data class VatBreakdownEntry(
    val rate: Int,
    val base: Long,
    val amount: Long
)
