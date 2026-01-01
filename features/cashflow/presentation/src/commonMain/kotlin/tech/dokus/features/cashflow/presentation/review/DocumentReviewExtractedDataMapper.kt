package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.model.ExtractedDocumentData

internal class DocumentReviewExtractedDataMapper {
    fun buildExtractedDataFromEditable(
        editable: EditableExtractedData,
        original: ExtractedDocumentData?
    ): ExtractedDocumentData {
        return ExtractedDocumentData(
            documentType = editable.documentType,
            rawText = original?.rawText,
            invoice = editable.invoice?.let { invoice ->
                original?.invoice?.copy(
                    clientName = invoice.clientName.takeIf { it.isNotBlank() },
                    clientVatNumber = invoice.clientVatNumber.takeIf { it.isNotBlank() },
                    clientEmail = invoice.clientEmail.takeIf { it.isNotBlank() },
                    clientAddress = invoice.clientAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = invoice.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = invoice.issueDate,
                    dueDate = invoice.dueDate,
                    items = invoice.items,
                    notes = invoice.notes.takeIf { it.isNotBlank() },
                    paymentTerms = invoice.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = invoice.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            bill = editable.bill?.let { bill ->
                original?.bill?.copy(
                    supplierName = bill.supplierName.takeIf { it.isNotBlank() },
                    supplierVatNumber = bill.supplierVatNumber.takeIf { it.isNotBlank() },
                    supplierAddress = bill.supplierAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = bill.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = bill.issueDate,
                    dueDate = bill.dueDate,
                    category = bill.category,
                    description = bill.description.takeIf { it.isNotBlank() },
                    notes = bill.notes.takeIf { it.isNotBlank() },
                    paymentTerms = bill.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = bill.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            expense = editable.expense?.let { expense ->
                original?.expense?.copy(
                    merchant = expense.merchant.takeIf { it.isNotBlank() },
                    merchantAddress = expense.merchantAddress.takeIf { it.isNotBlank() },
                    merchantVatNumber = expense.merchantVatNumber.takeIf { it.isNotBlank() },
                    date = expense.date,
                    category = expense.category,
                    description = expense.description.takeIf { it.isNotBlank() },
                    isDeductible = expense.isDeductible,
                    paymentMethod = expense.paymentMethod,
                    notes = expense.notes.takeIf { it.isNotBlank() },
                    receiptNumber = expense.receiptNumber.takeIf { it.isNotBlank() }
                )
            },
            overallConfidence = original?.overallConfidence,
            fieldConfidences = original?.fieldConfidences ?: emptyMap()
        )
    }
}
