@file:Suppress("CyclomaticComplexMethod") // Field mapping requires exhaustive when branches

package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentMethod

internal class DocumentReviewFieldEditor {
    suspend fun DocumentReviewCtx.handleUpdateInvoiceField(field: InvoiceField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState

            val updatedInvoice = when (field) {
                InvoiceField.CUSTOMER_NAME -> currentInvoice.copy(customerName = value as? String ?: "")
                InvoiceField.CUSTOMER_VAT_NUMBER -> currentInvoice.copy(customerVatNumber = value as? String ?: "")
                InvoiceField.CUSTOMER_EMAIL -> currentInvoice.copy(customerEmail = value as? String ?: "")
                InvoiceField.INVOICE_NUMBER -> currentInvoice.copy(invoiceNumber = value as? String ?: "")
                InvoiceField.ISSUE_DATE -> currentInvoice.copy(issueDate = value as? LocalDate)
                InvoiceField.DUE_DATE -> currentInvoice.copy(dueDate = value as? LocalDate)
                InvoiceField.SUBTOTAL_AMOUNT -> currentInvoice.copy(subtotalAmount = value as? String ?: "")
                InvoiceField.VAT_AMOUNT -> currentInvoice.copy(vatAmount = value as? String ?: "")
                InvoiceField.TOTAL_AMOUNT -> currentInvoice.copy(totalAmount = value as? String ?: "")
                InvoiceField.CURRENCY -> currentInvoice.copy(currency = value as? String ?: "EUR")
                InvoiceField.NOTES -> currentInvoice.copy(notes = value as? String ?: "")
                InvoiceField.IBAN -> currentInvoice.copy(iban = value as? String ?: "")
                InvoiceField.PAYMENT_REFERENCE -> currentInvoice.copy(paymentReference = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateBillField(field: BillField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentBill = editableData.bill ?: return@withState

            val updatedBill = when (field) {
                BillField.SUPPLIER_NAME -> currentBill.copy(supplierName = value as? String ?: "")
                BillField.SUPPLIER_VAT_NUMBER -> currentBill.copy(supplierVatNumber = value as? String ?: "")
                BillField.INVOICE_NUMBER -> currentBill.copy(invoiceNumber = value as? String ?: "")
                BillField.ISSUE_DATE -> currentBill.copy(issueDate = value as? LocalDate)
                BillField.DUE_DATE -> currentBill.copy(dueDate = value as? LocalDate)
                BillField.TOTAL_AMOUNT -> currentBill.copy(totalAmount = value as? String ?: "")
                BillField.VAT_AMOUNT -> currentBill.copy(vatAmount = value as? String ?: "")
                BillField.CURRENCY -> currentBill.copy(currency = value as? String ?: "EUR")
                BillField.NOTES -> currentBill.copy(notes = value as? String ?: "")
                BillField.IBAN -> currentBill.copy(iban = value as? String ?: "")
                BillField.PAYMENT_REFERENCE -> currentBill.copy(paymentReference = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(bill = updatedBill),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    /**
     * Handle Receipt field updates.
     */
    suspend fun DocumentReviewCtx.handleUpdateReceiptField(field: ReceiptField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentReceipt = editableData.receipt ?: return@withState

            val updatedReceipt = when (field) {
                ReceiptField.MERCHANT_NAME -> currentReceipt.copy(merchantName = value as? String ?: "")
                ReceiptField.MERCHANT_VAT_NUMBER -> currentReceipt.copy(merchantVatNumber = value as? String ?: "")
                ReceiptField.DATE -> currentReceipt.copy(date = value as? LocalDate)
                ReceiptField.TOTAL_AMOUNT -> currentReceipt.copy(totalAmount = value as? String ?: "")
                ReceiptField.VAT_AMOUNT -> currentReceipt.copy(vatAmount = value as? String ?: "")
                ReceiptField.CURRENCY -> currentReceipt.copy(currency = value as? String ?: "EUR")
                ReceiptField.RECEIPT_NUMBER -> currentReceipt.copy(receiptNumber = value as? String ?: "")
                ReceiptField.PAYMENT_METHOD -> currentReceipt.copy(paymentMethod = value as? PaymentMethod)
                ReceiptField.NOTES -> currentReceipt.copy(notes = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(receipt = updatedReceipt),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    /**
     * Handle CreditNote field updates.
     * CreditNote creates no cashflow on confirm - only on refund recording.
     */
    suspend fun DocumentReviewCtx.handleUpdateCreditNoteField(field: CreditNoteField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentCreditNote = editableData.creditNote ?: return@withState

            val updatedCreditNote = when (field) {
                CreditNoteField.COUNTERPARTY_NAME -> currentCreditNote.copy(counterpartyName = value as? String ?: "")
                CreditNoteField.COUNTERPARTY_VAT_NUMBER -> currentCreditNote.copy(
                    counterpartyVatNumber = value as? String ?: ""
                )
                CreditNoteField.CREDIT_NOTE_NUMBER -> currentCreditNote.copy(creditNoteNumber = value as? String ?: "")
                CreditNoteField.ORIGINAL_INVOICE_NUMBER -> currentCreditNote.copy(
                    originalInvoiceNumber = value as? String ?: ""
                )
                CreditNoteField.ISSUE_DATE -> currentCreditNote.copy(issueDate = value as? LocalDate)
                CreditNoteField.SUBTOTAL_AMOUNT -> currentCreditNote.copy(subtotalAmount = value as? String ?: "")
                CreditNoteField.VAT_AMOUNT -> currentCreditNote.copy(vatAmount = value as? String ?: "")
                CreditNoteField.TOTAL_AMOUNT -> currentCreditNote.copy(totalAmount = value as? String ?: "")
                CreditNoteField.CURRENCY -> currentCreditNote.copy(currency = value as? String ?: "EUR")
                CreditNoteField.REASON -> currentCreditNote.copy(reason = value as? String ?: "")
                CreditNoteField.NOTES -> currentCreditNote.copy(notes = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(creditNote = updatedCreditNote),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    /**
     * Handle manual document type selection.
     * This is used when the AI fails to classify the document or the type is Unknown.
     * Initializes empty fields for the selected type.
     */
    suspend fun DocumentReviewCtx.handleSelectDocumentType(type: DocumentType) {
        withState<DocumentReviewState.Content, _> {
            // Don't allow selecting Unknown type manually
            if (type == DocumentType.Unknown) return@withState

            val newEditableData = when (type) {
                DocumentType.Invoice -> EditableExtractedData(
                    documentType = type,
                    invoice = EditableInvoiceFields(),
                )
                DocumentType.Bill -> EditableExtractedData(
                    documentType = type,
                    bill = EditableBillFields(),
                )
                DocumentType.Receipt -> EditableExtractedData(
                    documentType = type,
                    receipt = EditableReceiptFields(),
                )
                DocumentType.CreditNote -> EditableExtractedData(
                    documentType = type,
                    creditNote = EditableCreditNoteFields(),
                )
                else -> return@withState
            }

            updateState {
                copy(
                    editableData = newEditableData,
                    hasUnsavedChanges = true,
                )
            }
        }
    }
}
