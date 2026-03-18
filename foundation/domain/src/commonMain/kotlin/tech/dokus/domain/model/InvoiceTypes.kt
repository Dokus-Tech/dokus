package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId

/**
 * Invoice line item DTO — used by backend confirmation services.
 * Frontend uses [DocLineItem] instead.
 */
@Serializable
data class InvoiceItemDto(
    val id: String? = null,
    val invoiceId: InvoiceId? = null,
    val description: String,
    val quantity: Double,
    val unitPrice: Money,
    val vatRate: VatRate,
    val lineTotal: Money,
    val vatAmount: Money,
    val sortOrder: Int = 0,
)

/** Peppol e-invoicing info — present when invoice is sent via Peppol. */
@Serializable
data class InvoicePeppolInfo(
    val peppolId: PeppolId,
    val sentAt: LocalDateTime,
    val status: PeppolStatus,
)

/** Payment link info — present when a payment link was generated. */
@Serializable
data class PaymentLinkInfo(
    val url: String,
    val expiresAt: LocalDateTime? = null,
)

/** Payment info — present when invoice is paid. */
@Serializable
data class InvoicePaymentInfo(
    val paidAt: LocalDateTime,
    val paymentMethod: PaymentMethod,
)

/** Refund claim DTO — tracks expected refunds from credit notes. */
@Serializable
data class RefundClaimDto(
    val id: RefundClaimId,
    val tenantId: TenantId,
    val creditNoteId: CreditNoteId,
    val counterpartyId: ContactId,
    val amount: Money,
    val currency: Currency,
    val expectedDate: LocalDate? = null,
    val status: RefundClaimStatus,
    val settledAt: LocalDateTime? = null,
    val cashflowEntryId: CashflowEntryId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
