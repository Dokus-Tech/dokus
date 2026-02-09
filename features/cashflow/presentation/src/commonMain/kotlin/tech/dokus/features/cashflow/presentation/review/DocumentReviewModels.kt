package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.VatBreakdownEntry

@Immutable
data class EditableExtractedData(
    val documentType: DocumentType,
    val invoice: EditableInvoiceFields? = null,
    val bill: EditableBillFields? = null,
    val receipt: EditableReceiptFields? = null,
    val creditNote: EditableCreditNoteFields? = null,
) {
    val isValid: Boolean
        get() = when (documentType) {
            DocumentType.Invoice -> invoice?.isValid == true
            DocumentType.Bill -> bill?.isValid == true
            DocumentType.Receipt -> receipt?.isValid == true
            DocumentType.CreditNote -> creditNote?.isValid == true
            else -> false
        }

    companion object {
        fun fromDraftData(data: DocumentDraftData?): EditableExtractedData {
            val type = when (data) {
                is InvoiceDraftData -> DocumentType.Invoice
                is BillDraftData -> DocumentType.Bill
                is ReceiptDraftData -> DocumentType.Receipt
                is CreditNoteDraftData -> DocumentType.CreditNote
                null -> DocumentType.Unknown
            }
            return EditableExtractedData(
                documentType = type,
                invoice = (data as? InvoiceDraftData)?.let { EditableInvoiceFields.fromDraft(it) },
                bill = (data as? BillDraftData)?.let { EditableBillFields.fromDraft(it) },
                receipt = (data as? ReceiptDraftData)?.let { EditableReceiptFields.fromDraft(it) },
                creditNote = (data as? CreditNoteDraftData)?.let { EditableCreditNoteFields.fromDraft(it) },
            )
        }
    }
}

@Immutable
data class EditableInvoiceFields(
    val customerName: String = "",
    val customerVatNumber: String = "",
    val customerEmail: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val iban: String = "",
    val paymentReference: String = "",
    val notes: String = "",
) {
    val isValid: Boolean
        get() = issueDate != null && subtotalAmount.isNotBlank()

    companion object {
        fun fromDraft(data: InvoiceDraftData): EditableInvoiceFields {
            return EditableInvoiceFields(
                customerName = data.customerName ?: "",
                customerVatNumber = data.customerVat?.value ?: "",
                customerEmail = data.customerEmail?.value ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                lineItems = data.lineItems,
                vatBreakdown = data.vatBreakdown,
                subtotalAmount = data.subtotalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                currency = data.currency.name,
                iban = data.iban?.value ?: "",
                paymentReference = data.payment?.structuredComm?.value ?: data.payment?.reference.orEmpty(),
                notes = data.notes ?: "",
            )
        }
    }
}

@Immutable
data class EditableBillFields(
    val supplierName: String = "",
    val supplierVatNumber: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val totalAmount: String = "",
    val vatAmount: String = "",
    val currency: String = "EUR",
    val iban: String = "",
    val paymentReference: String = "",
    val notes: String = "",
) {
    val isValid: Boolean
        get() = supplierName.isNotBlank() && issueDate != null && totalAmount.isNotBlank()

    companion object {
        fun fromDraft(data: BillDraftData): EditableBillFields {
            return EditableBillFields(
                supplierName = data.supplierName ?: "",
                supplierVatNumber = data.supplierVat?.value ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                lineItems = data.lineItems,
                vatBreakdown = data.vatBreakdown,
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                currency = data.currency.name,
                iban = data.iban?.value ?: "",
                paymentReference = data.payment?.structuredComm?.value ?: data.payment?.reference.orEmpty(),
                notes = data.notes ?: "",
            )
        }
    }
}

@Immutable
data class EditableReceiptFields(
    val merchantName: String = "",
    val merchantVatNumber: String = "",
    val date: LocalDate? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val totalAmount: String = "",
    val vatAmount: String = "",
    val currency: String = "EUR",
    val paymentMethod: PaymentMethod? = null,
    val notes: String = "",
    val receiptNumber: String = "",
) {
    val isValid: Boolean
        get() = merchantName.isNotBlank() && date != null && totalAmount.isNotBlank()

    companion object {
        fun fromDraft(data: ReceiptDraftData): EditableReceiptFields {
            return EditableReceiptFields(
                merchantName = data.merchantName ?: "",
                merchantVatNumber = data.merchantVat?.value ?: "",
                date = data.date,
                lineItems = data.lineItems,
                vatBreakdown = data.vatBreakdown,
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                currency = data.currency.name,
                paymentMethod = data.paymentMethod,
                notes = data.notes ?: "",
                receiptNumber = data.receiptNumber ?: "",
            )
        }
    }
}

@Immutable
data class EditableCreditNoteFields(
    val counterpartyName: String = "",
    val counterpartyVatNumber: String = "",
    val creditNoteNumber: String = "",
    val originalInvoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val lineItems: List<FinancialLineItem> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntry> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val reason: String = "",
    val notes: String = "",
) {
    val isValid: Boolean
        get() = counterpartyName.isNotBlank() && issueDate != null && subtotalAmount.isNotBlank()

    companion object {
        fun fromDraft(data: CreditNoteDraftData): EditableCreditNoteFields {
            return EditableCreditNoteFields(
                counterpartyName = data.counterpartyName ?: "",
                counterpartyVatNumber = data.counterpartyVat?.value ?: "",
                creditNoteNumber = data.creditNoteNumber ?: "",
                originalInvoiceNumber = data.originalInvoiceNumber ?: "",
                issueDate = data.issueDate,
                lineItems = data.lineItems,
                vatBreakdown = data.vatBreakdown,
                subtotalAmount = data.subtotalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                currency = data.currency.name,
                reason = data.reason ?: "",
                notes = data.notes ?: "",
            )
        }
    }
}
