package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.Quantity
import tech.dokus.domain.VatRate

/**
 * Unified line item used across both Draft and Confirmed document states.
 * Replaces [FinancialLineItem] (draft, Long-based) and [InvoiceItemDto] (confirmed, Money-based).
 *
 * For Draft: fields may be null (AI didn't extract them).
 * For Confirmed: all financial fields are populated by the confirmation service.
 */
@Serializable
data class DocLineItem(
    val description: String,
    val quantity: Quantity? = null,
    val unitPrice: Money? = null,
    val vatRate: VatRate? = null,
    val netAmount: Money? = null,
    val vatAmount: Money? = null,
    val sortOrder: Int = 0,
)
