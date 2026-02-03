@file:Suppress("CyclomaticComplexMethod") // Field mapping requires exhaustive when branches

package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod

internal class DocumentReviewFieldEditor {
    suspend fun DocumentReviewCtx.handleUpdateInvoiceField(field: InvoiceField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState

            val updatedInvoice = when (field) {
                InvoiceField.CLIENT_NAME -> currentInvoice.copy(clientName = value as? String ?: "")
                InvoiceField.CLIENT_VAT_NUMBER -> currentInvoice.copy(clientVatNumber = value as? String ?: "")
                InvoiceField.CLIENT_EMAIL -> currentInvoice.copy(clientEmail = value as? String ?: "")
                InvoiceField.CLIENT_ADDRESS -> currentInvoice.copy(clientAddress = value as? String ?: "")
                InvoiceField.INVOICE_NUMBER -> currentInvoice.copy(invoiceNumber = value as? String ?: "")
                InvoiceField.ISSUE_DATE -> currentInvoice.copy(issueDate = value as? LocalDate)
                InvoiceField.DUE_DATE -> currentInvoice.copy(dueDate = value as? LocalDate)
                InvoiceField.SUBTOTAL_AMOUNT -> currentInvoice.copy(subtotalAmount = value as? String ?: "")
                InvoiceField.VAT_AMOUNT -> currentInvoice.copy(vatAmount = value as? String ?: "")
                InvoiceField.TOTAL_AMOUNT -> currentInvoice.copy(totalAmount = value as? String ?: "")
                InvoiceField.CURRENCY -> currentInvoice.copy(currency = value as? String ?: "EUR")
                InvoiceField.NOTES -> currentInvoice.copy(notes = value as? String ?: "")
                InvoiceField.PAYMENT_TERMS -> currentInvoice.copy(paymentTerms = value as? String ?: "")
                InvoiceField.BANK_ACCOUNT -> currentInvoice.copy(bankAccount = value as? String ?: "")
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
                BillField.SUPPLIER_ADDRESS -> currentBill.copy(supplierAddress = value as? String ?: "")
                BillField.INVOICE_NUMBER -> currentBill.copy(invoiceNumber = value as? String ?: "")
                BillField.ISSUE_DATE -> currentBill.copy(issueDate = value as? LocalDate)
                BillField.DUE_DATE -> currentBill.copy(dueDate = value as? LocalDate)
                BillField.AMOUNT -> currentBill.copy(amount = value as? String ?: "")
                BillField.VAT_AMOUNT -> currentBill.copy(vatAmount = value as? String ?: "")
                BillField.VAT_RATE -> currentBill.copy(vatRate = value as? String ?: "")
                BillField.CURRENCY -> currentBill.copy(currency = value as? String ?: "EUR")
                BillField.CATEGORY -> currentBill.copy(category = value as? ExpenseCategory)
                BillField.DESCRIPTION -> currentBill.copy(description = value as? String ?: "")
                BillField.NOTES -> currentBill.copy(notes = value as? String ?: "")
                BillField.PAYMENT_TERMS -> currentBill.copy(paymentTerms = value as? String ?: "")
                BillField.BANK_ACCOUNT -> currentBill.copy(bankAccount = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(bill = updatedBill),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateExpenseField(field: ExpenseField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentExpense = editableData.expense ?: return@withState

            val updatedExpense = when (field) {
                ExpenseField.MERCHANT -> currentExpense.copy(merchant = value as? String ?: "")
                ExpenseField.MERCHANT_ADDRESS -> currentExpense.copy(merchantAddress = value as? String ?: "")
                ExpenseField.MERCHANT_VAT_NUMBER -> currentExpense.copy(merchantVatNumber = value as? String ?: "")
                ExpenseField.DATE -> currentExpense.copy(date = value as? LocalDate)
                ExpenseField.AMOUNT -> currentExpense.copy(amount = value as? String ?: "")
                ExpenseField.VAT_AMOUNT -> currentExpense.copy(vatAmount = value as? String ?: "")
                ExpenseField.VAT_RATE -> currentExpense.copy(vatRate = value as? String ?: "")
                ExpenseField.CURRENCY -> currentExpense.copy(currency = value as? String ?: "EUR")
                ExpenseField.CATEGORY -> currentExpense.copy(category = value as? ExpenseCategory)
                ExpenseField.DESCRIPTION -> currentExpense.copy(description = value as? String ?: "")
                ExpenseField.IS_DEDUCTIBLE -> currentExpense.copy(isDeductible = value as? Boolean ?: true)
                ExpenseField.DEDUCTIBLE_PERCENTAGE -> currentExpense.copy(
                    deductiblePercentage = value as? String ?: "100"
                )
                ExpenseField.PAYMENT_METHOD -> currentExpense.copy(paymentMethod = value as? PaymentMethod)
                ExpenseField.NOTES -> currentExpense.copy(notes = value as? String ?: "")
                ExpenseField.RECEIPT_NUMBER -> currentExpense.copy(receiptNumber = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(expense = updatedExpense),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    /**
     * Handle Receipt field updates.
     * Receipt mirrors Expense structure since it confirms into Expense.
     */
    suspend fun DocumentReviewCtx.handleUpdateReceiptField(field: ReceiptField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentReceipt = editableData.receipt ?: return@withState

            val updatedReceipt = when (field) {
                ReceiptField.MERCHANT -> currentReceipt.copy(merchant = value as? String ?: "")
                ReceiptField.MERCHANT_ADDRESS -> currentReceipt.copy(merchantAddress = value as? String ?: "")
                ReceiptField.MERCHANT_VAT_NUMBER -> currentReceipt.copy(merchantVatNumber = value as? String ?: "")
                ReceiptField.DATE -> currentReceipt.copy(date = value as? LocalDate)
                ReceiptField.AMOUNT -> currentReceipt.copy(amount = value as? String ?: "")
                ReceiptField.VAT_AMOUNT -> currentReceipt.copy(vatAmount = value as? String ?: "")
                ReceiptField.VAT_RATE -> currentReceipt.copy(vatRate = value as? String ?: "")
                ReceiptField.CURRENCY -> currentReceipt.copy(currency = value as? String ?: "EUR")
                ReceiptField.CATEGORY -> currentReceipt.copy(category = value as? ExpenseCategory)
                ReceiptField.DESCRIPTION -> currentReceipt.copy(description = value as? String ?: "")
                ReceiptField.IS_DEDUCTIBLE -> currentReceipt.copy(isDeductible = value as? Boolean ?: true)
                ReceiptField.DEDUCTIBLE_PERCENTAGE -> currentReceipt.copy(
                    deductiblePercentage = value as? String ?: "100"
                )
                ReceiptField.PAYMENT_METHOD -> currentReceipt.copy(paymentMethod = value as? PaymentMethod)
                ReceiptField.NOTES -> currentReceipt.copy(notes = value as? String ?: "")
                ReceiptField.RECEIPT_NUMBER -> currentReceipt.copy(receiptNumber = value as? String ?: "")
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
     * Handle ProForma field updates.
     * ProForma is informational only - no cashflow impact.
     */
    suspend fun DocumentReviewCtx.handleUpdateProFormaField(field: ProFormaField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentProForma = editableData.proForma ?: return@withState

            val updatedProForma = when (field) {
                ProFormaField.CLIENT_NAME -> currentProForma.copy(clientName = value as? String ?: "")
                ProFormaField.CLIENT_VAT_NUMBER -> currentProForma.copy(clientVatNumber = value as? String ?: "")
                ProFormaField.CLIENT_EMAIL -> currentProForma.copy(clientEmail = value as? String ?: "")
                ProFormaField.CLIENT_ADDRESS -> currentProForma.copy(clientAddress = value as? String ?: "")
                ProFormaField.PRO_FORMA_NUMBER -> currentProForma.copy(proFormaNumber = value as? String ?: "")
                ProFormaField.ISSUE_DATE -> currentProForma.copy(issueDate = value as? LocalDate)
                ProFormaField.VALID_UNTIL -> currentProForma.copy(validUntil = value as? LocalDate)
                ProFormaField.SUBTOTAL_AMOUNT -> currentProForma.copy(subtotalAmount = value as? String ?: "")
                ProFormaField.VAT_AMOUNT -> currentProForma.copy(vatAmount = value as? String ?: "")
                ProFormaField.TOTAL_AMOUNT -> currentProForma.copy(totalAmount = value as? String ?: "")
                ProFormaField.CURRENCY -> currentProForma.copy(currency = value as? String ?: "EUR")
                ProFormaField.NOTES -> currentProForma.copy(notes = value as? String ?: "")
                ProFormaField.TERMS_AND_CONDITIONS -> currentProForma.copy(termsAndConditions = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(proForma = updatedProForma),
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
                CreditNoteField.COUNTERPARTY_ADDRESS -> currentCreditNote.copy(
                    counterpartyAddress = value as? String ?: ""
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
                DocumentType.ProForma -> EditableExtractedData(
                    documentType = type,
                    proForma = EditableProFormaFields(),
                )
                DocumentType.CreditNote -> EditableExtractedData(
                    documentType = type,
                    creditNote = EditableCreditNoteFields(),
                )
                DocumentType.Unknown -> return@withState
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
