package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Email
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CreditNoteDirection
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentKind
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber

/**
 * Canonical, normalized draft data shown to users and used for confirmation.
 * This is deliberately AI-agnostic: no prompt/tool metadata, no token usage.
 */
@Serializable
sealed interface DocumentDraftData {
    val kind: DocumentKind
}

@Serializable
@SerialName("invoice_draft")
data class InvoiceDraftData(
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val currency: Currency = Currency.default,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val customerName: String? = null,
    val customerVat: VatNumber? = null,
    val customerEmail: Email? = null,
    val iban: Iban? = null,
    val payment: CanonicalPayment? = null,
    val notes: String? = null
) : DocumentDraftData {
    override val kind: DocumentKind = DocumentKind.Invoice
}

@Serializable
@SerialName("bill_draft")
data class BillDraftData(
    val supplierName: String? = null,
    val supplierVat: VatNumber? = null,
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
    val notes: String? = null
) : DocumentDraftData {
    override val kind: DocumentKind = DocumentKind.Bill
}

@Serializable
@SerialName("credit_note_draft")
data class CreditNoteDraftData(
    val creditNoteNumber: String? = null,
    val direction: CreditNoteDirection = CreditNoteDirection.Unknown,
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
    val notes: String? = null
) : DocumentDraftData {
    override val kind: DocumentKind = DocumentKind.CreditNote
}

@Serializable
@SerialName("receipt_draft")
data class ReceiptDraftData(
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
    override val kind: DocumentKind = DocumentKind.Receipt
}
