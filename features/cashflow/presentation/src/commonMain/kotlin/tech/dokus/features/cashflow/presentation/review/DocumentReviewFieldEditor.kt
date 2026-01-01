package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.dsl.withState
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
                ExpenseField.DEDUCTIBLE_PERCENTAGE -> currentExpense.copy(deductiblePercentage = value as? String ?: "100")
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
}
