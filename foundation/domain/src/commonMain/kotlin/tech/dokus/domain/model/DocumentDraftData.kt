package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Email
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber

/**
 * Canonical, normalized draft data shown to users and used for confirmation.
 * This is deliberately AI-agnostic: no prompt/tool metadata, no token usage.
 */
@Serializable
sealed interface DocumentDraftData

fun DocumentDraftData.toDocumentType(): DocumentType = when (this) {
    is InvoiceDraftData -> DocumentType.Invoice
    is CreditNoteDraftData -> DocumentType.CreditNote
    is ReceiptDraftData -> DocumentType.Receipt
}

@Serializable
data class PartyDraft(
    val name: String? = null,
    val vat: VatNumber? = null,
    val email: Email? = null,
    val iban: Iban? = null,
    val streetLine1: String? = null,
    val streetLine2: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: String? = null,
)

@Serializable
@SerialName("invoice_draft")
data class InvoiceDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val currency: Currency = Currency.default,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val iban: Iban? = null,
    val payment: CanonicalPayment? = null,
    val notes: String? = null,
    // Neutral party model used for deterministic direction and counterparty resolution.
    val seller: PartyDraft = PartyDraft(),
    val buyer: PartyDraft = PartyDraft(),
) : DocumentDraftData {
}

@Serializable
@SerialName("credit_note_draft")
data class CreditNoteDraftData(
    val creditNoteNumber: String? = null,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val issueDate: LocalDate? = null,
    val currency: Currency = Currency.default,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val counterpartyName: String? = null,
    val counterpartyVat: VatNumber? = null,
    val originalInvoiceNumber: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    // Neutral party model used for deterministic direction and counterparty resolution.
    val seller: PartyDraft = PartyDraft(),
    val buyer: PartyDraft = PartyDraft(),
) : DocumentDraftData {
}

@Serializable
@SerialName("receipt_draft")
data class ReceiptDraftData(
    val direction: DocumentDirection = DocumentDirection.Inbound,
    val merchantName: String? = null,
    val merchantVat: VatNumber? = null,
    val date: LocalDate? = null,
    val currency: Currency = Currency.default,
    val totalAmount: Money? = null,
    val vatAmount: Money? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val receiptNumber: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null
) : DocumentDraftData {
}
