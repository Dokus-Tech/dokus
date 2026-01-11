package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedCreditNoteFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedExpenseFields
import tech.dokus.domain.model.ExtractedInvoiceFields
import tech.dokus.domain.model.ExtractedLineItem
import tech.dokus.domain.model.ExtractedProFormaFields
import tech.dokus.domain.model.ExtractedReceiptFields

@Immutable
data class EditableExtractedData(
    val documentType: DocumentType,
    val invoice: EditableInvoiceFields? = null,
    val bill: EditableBillFields? = null,
    val expense: EditableExpenseFields? = null,
    val receipt: EditableReceiptFields? = null,
    val proForma: EditableProFormaFields? = null,
    val creditNote: EditableCreditNoteFields? = null,
) {
    val isValid: Boolean
        get() = when (documentType) {
            DocumentType.Invoice -> invoice?.isValid == true
            DocumentType.Bill -> bill?.isValid == true
            DocumentType.Expense -> expense?.isValid == true
            DocumentType.Receipt -> receipt?.isValid == true
            DocumentType.ProForma -> proForma?.isValid == true
            DocumentType.CreditNote -> creditNote?.isValid == true
            DocumentType.Unknown -> false
        }

    companion object {
        fun fromExtractedData(data: ExtractedDocumentData?): EditableExtractedData {
            val type = data?.documentType ?: DocumentType.Unknown
            return EditableExtractedData(
                documentType = type,
                invoice = data?.invoice?.let { EditableInvoiceFields.fromExtracted(it) },
                bill = data?.bill?.let { EditableBillFields.fromExtracted(it) },
                expense = data?.expense?.let { EditableExpenseFields.fromExtracted(it) },
                receipt = data?.receipt?.let { EditableReceiptFields.fromExtracted(it) },
                proForma = data?.proForma?.let { EditableProFormaFields.fromExtracted(it) },
                creditNote = data?.creditNote?.let { EditableCreditNoteFields.fromExtracted(it) },
            )
        }
    }
}

@Immutable
data class EditableInvoiceFields(
    val clientName: String = "",
    val clientVatNumber: String = "",
    val clientEmail: String = "",
    val clientAddress: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val items: List<ExtractedLineItem> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val notes: String = "",
    val paymentTerms: String = "",
    val bankAccount: String = "",
) {
    val isValid: Boolean
        get() = issueDate != null &&
            subtotalAmount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedInvoiceFields): EditableInvoiceFields {
            return EditableInvoiceFields(
                clientName = data.clientName ?: "",
                clientVatNumber = data.clientVatNumber ?: "",
                clientEmail = data.clientEmail ?: "",
                clientAddress = data.clientAddress ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                items = data.items ?: emptyList(),
                subtotalAmount = data.subtotalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                notes = data.notes ?: "",
                paymentTerms = data.paymentTerms ?: "",
                bankAccount = data.bankAccount ?: "",
            )
        }
    }
}

@Immutable
data class EditableBillFields(
    val supplierName: String = "",
    val supplierVatNumber: String = "",
    val supplierAddress: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val amount: String = "",
    val vatAmount: String = "",
    val vatRate: String = "",
    val currency: String = "EUR",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val notes: String = "",
    val paymentTerms: String = "",
    val bankAccount: String = "",
) {
    val isValid: Boolean
        get() = supplierName.isNotBlank() &&
            issueDate != null &&
            amount.isNotBlank() &&
            category != null

    companion object {
        fun fromExtracted(data: ExtractedBillFields): EditableBillFields {
            return EditableBillFields(
                supplierName = data.supplierName ?: "",
                supplierVatNumber = data.supplierVatNumber ?: "",
                supplierAddress = data.supplierAddress ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                amount = data.amount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                vatRate = data.vatRate?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                category = data.category,
                description = data.description ?: "",
                notes = data.notes ?: "",
                paymentTerms = data.paymentTerms ?: "",
                bankAccount = data.bankAccount ?: "",
            )
        }
    }
}

@Immutable
data class EditableExpenseFields(
    val merchant: String = "",
    val merchantAddress: String = "",
    val merchantVatNumber: String = "",
    val date: LocalDate? = null,
    val amount: String = "",
    val vatAmount: String = "",
    val vatRate: String = "",
    val currency: String = "EUR",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val isDeductible: Boolean = true,
    val deductiblePercentage: String = "100",
    val paymentMethod: PaymentMethod? = null,
    val notes: String = "",
    val receiptNumber: String = "",
) {
    val isValid: Boolean
        get() = merchant.isNotBlank() &&
            date != null &&
            amount.isNotBlank() &&
            category != null

    companion object {
        fun fromExtracted(data: ExtractedExpenseFields): EditableExpenseFields {
            return EditableExpenseFields(
                merchant = data.merchant ?: "",
                merchantAddress = data.merchantAddress ?: "",
                merchantVatNumber = data.merchantVatNumber ?: "",
                date = data.date,
                amount = data.amount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                vatRate = data.vatRate?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                category = data.category,
                description = data.description ?: "",
                isDeductible = data.isDeductible ?: true,
                deductiblePercentage = data.deductiblePercentage?.toDisplayString() ?: "100",
                paymentMethod = data.paymentMethod,
                notes = data.notes ?: "",
                receiptNumber = data.receiptNumber ?: "",
            )
        }
    }
}

/**
 * Editable Receipt fields - mirrors Expense structure since Receipt confirms into Expense.
 * No line items (totals-first: merchant, date, total, VAT).
 */
@Immutable
data class EditableReceiptFields(
    val merchant: String = "",
    val merchantAddress: String = "",
    val merchantVatNumber: String = "",
    val date: LocalDate? = null,
    val amount: String = "",
    val vatAmount: String = "",
    val vatRate: String = "",
    val currency: String = "EUR",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val isDeductible: Boolean = true,
    val deductiblePercentage: String = "100",
    val paymentMethod: PaymentMethod? = null,
    val notes: String = "",
    val receiptNumber: String = "",
) {
    val isValid: Boolean
        get() = merchant.isNotBlank() &&
            date != null &&
            amount.isNotBlank() &&
            category != null

    companion object {
        fun fromExtracted(data: ExtractedReceiptFields): EditableReceiptFields {
            return EditableReceiptFields(
                merchant = data.merchant ?: "",
                merchantAddress = data.merchantAddress ?: "",
                merchantVatNumber = data.merchantVatNumber ?: "",
                date = data.date,
                amount = data.amount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                vatRate = data.vatRate?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                category = data.category,
                description = data.description ?: "",
                isDeductible = data.isDeductible ?: true,
                deductiblePercentage = data.deductiblePercentage?.toDisplayString() ?: "100",
                paymentMethod = data.paymentMethod,
                notes = data.notes ?: "",
                receiptNumber = data.receiptNumber ?: "",
            )
        }
    }
}

/**
 * Editable ProForma fields - informational only, no cashflow impact.
 * Can be converted to Invoice via explicit action.
 */
@Immutable
data class EditableProFormaFields(
    val clientName: String = "",
    val clientVatNumber: String = "",
    val clientEmail: String = "",
    val clientAddress: String = "",
    val selectedContactId: ContactId? = null,
    val proFormaNumber: String = "",
    val issueDate: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val items: List<ExtractedLineItem> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val notes: String = "",
    val termsAndConditions: String = "",
) {
    val isValid: Boolean
        get() = clientName.isNotBlank() &&
            issueDate != null &&
            totalAmount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedProFormaFields): EditableProFormaFields {
            return EditableProFormaFields(
                clientName = data.clientName ?: "",
                clientVatNumber = data.clientVatNumber ?: "",
                clientEmail = data.clientEmail ?: "",
                clientAddress = data.clientAddress ?: "",
                proFormaNumber = data.proFormaNumber ?: "",
                issueDate = data.issueDate,
                validUntil = data.validUntil,
                items = data.items ?: emptyList(),
                subtotalAmount = data.subtotalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                notes = data.notes ?: "",
                termsAndConditions = data.termsAndConditions ?: "",
            )
        }
    }
}

/**
 * Editable CreditNote fields.
 * CreditNote adjusts accounting totals but creates NO cashflow entry on confirmation.
 * Cashflow is created only when refund payment is recorded.
 */
@Immutable
data class EditableCreditNoteFields(
    val counterpartyName: String = "",
    val counterpartyVatNumber: String = "",
    val counterpartyAddress: String = "",
    val selectedContactId: ContactId? = null,
    val creditNoteNumber: String = "",
    val originalInvoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val items: List<ExtractedLineItem> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val reason: String = "",
    val notes: String = "",
) {
    val isValid: Boolean
        get() = counterpartyName.isNotBlank() &&
            issueDate != null &&
            totalAmount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedCreditNoteFields): EditableCreditNoteFields {
            return EditableCreditNoteFields(
                counterpartyName = data.counterpartyName ?: "",
                counterpartyVatNumber = data.counterpartyVatNumber ?: "",
                counterpartyAddress = data.counterpartyAddress ?: "",
                creditNoteNumber = data.creditNoteNumber ?: "",
                originalInvoiceNumber = data.originalInvoiceNumber ?: "",
                issueDate = data.issueDate,
                items = data.items ?: emptyList(),
                subtotalAmount = data.subtotalAmount?.toDisplayString() ?: "",
                vatAmount = data.vatAmount?.toDisplayString() ?: "",
                totalAmount = data.totalAmount?.toDisplayString() ?: "",
                currency = data.currency?.name ?: "EUR",
                reason = data.reason ?: "",
                notes = data.notes ?: "",
            )
        }
    }
}

@Immutable
data class ContactSuggestion(
    val contactId: ContactId,
    val name: String,
    val vatNumber: String?,
    val matchConfidence: Float,
    val matchReason: ContactSuggestionReason,
)

@Immutable
sealed interface ContactSuggestionReason {
    data object AiSuggested : ContactSuggestionReason
    data class Custom(val value: String) : ContactSuggestionReason
}

@Immutable
data class ContactSnapshot(
    val id: ContactId,
    val name: String,
    val vatNumber: String?,
    val email: String?,
)

@Immutable
sealed interface ContactSelectionState {
    data object NoContact : ContactSelectionState
    data class Suggested(
        val contactId: ContactId,
        val name: String,
        val vatNumber: String?,
        val confidence: Float,
        val reason: ContactSuggestionReason,
    ) : ContactSelectionState
    data object Selected : ContactSelectionState
}
