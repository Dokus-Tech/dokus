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
import tech.dokus.domain.model.contact.CounterpartySnapshot

/**
 * Canonical, normalized draft data shown to users and used for confirmation.
 * This is deliberately AI-agnostic: no prompt/tool metadata, no token usage.
 */
@Serializable
sealed interface DocumentDraftData

fun DocumentDraftData.toDirection(): DocumentDirection = when (this) {
    is InvoiceDraftData -> direction
    is CreditNoteDraftData -> direction
    is ReceiptDraftData -> direction
    is BankStatementDraftData -> direction
}

fun DocumentDraftData.toDocumentType(): DocumentType = when (this) {
    is InvoiceDraftData -> DocumentType.Invoice
    is CreditNoteDraftData -> DocumentType.CreditNote
    is ReceiptDraftData -> DocumentType.Receipt
    is BankStatementDraftData -> DocumentType.BankStatement
}

fun DocumentDraftData.toTotalAmount(): Money? = when (this) {
    is InvoiceDraftData -> totalAmount
    is CreditNoteDraftData -> totalAmount
    is ReceiptDraftData -> totalAmount
    is BankStatementDraftData -> null
}

fun DocumentDraftData.toCurrency(): Currency = when (this) {
    is InvoiceDraftData -> currency
    is CreditNoteDraftData -> currency
    is ReceiptDraftData -> currency
    is BankStatementDraftData -> Currency.default
}

fun DocumentDraftData.toSortDate(): LocalDate? = when (this) {
    is InvoiceDraftData -> issueDate
    is CreditNoteDraftData -> issueDate
    is ReceiptDraftData -> date
    is BankStatementDraftData -> periodEnd
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
    val originalInvoiceNumber: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    // Neutral party model used for deterministic direction and counterparty resolution.
    val seller: PartyDraft = PartyDraft(),
    val buyer: PartyDraft = PartyDraft(),
) : DocumentDraftData

val CreditNoteDraftData.resolvedCounterpartyName: String?
    get() = when (direction) {
        DocumentDirection.Inbound -> seller.name
        DocumentDirection.Outbound -> buyer.name
        else -> seller.name ?: buyer.name
    }

val CreditNoteDraftData.resolvedCounterpartyVat: VatNumber?
    get() = when (direction) {
        DocumentDirection.Inbound -> seller.vat
        DocumentDirection.Outbound -> buyer.vat
        else -> seller.vat ?: buyer.vat
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

@Serializable
data class BankStatementTransactionDraftRow(
    val transactionDate: LocalDate? = null,
    val signedAmount: Money? = null,
    val counterparty: CounterpartySnapshot = CounterpartySnapshot(),
    val communication: TransactionCommunication? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double = 0.0,
    val largeAmountFlag: Boolean = false,
)

@Serializable
@SerialName("bank_statement_draft")
data class BankStatementDraftData(
    val direction: DocumentDirection = DocumentDirection.Neutral,
    val transactions: List<BankStatementTransactionDraftRow> = emptyList(),
    val accountIban: Iban? = null,
    val openingBalance: Money? = null,
    val closingBalance: Money? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val notes: String? = null,
) : DocumentDraftData
