package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
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
                val base = original?.invoice ?: tech.dokus.domain.model.ExtractedInvoiceFields()
                base.copy(
                    clientName = invoice.clientName.takeIf { it.isNotBlank() },
                    clientVatNumber = invoice.clientVatNumber.takeIf { it.isNotBlank() },
                    clientEmail = invoice.clientEmail.takeIf { it.isNotBlank() },
                    clientAddress = invoice.clientAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = invoice.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = invoice.issueDate,
                    dueDate = invoice.dueDate,
                    items = invoice.items,
                    subtotalAmount = Money.parse(invoice.subtotalAmount),
                    vatAmount = Money.parse(invoice.vatAmount),
                    totalAmount = Money.parse(invoice.totalAmount),
                    currency = parseCurrency(invoice.currency, original?.invoice?.currency),
                    notes = invoice.notes.takeIf { it.isNotBlank() },
                    paymentTerms = invoice.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = invoice.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            bill = editable.bill?.let { bill ->
                val base = original?.bill ?: tech.dokus.domain.model.ExtractedBillFields()
                base.copy(
                    supplierName = bill.supplierName.takeIf { it.isNotBlank() },
                    supplierVatNumber = bill.supplierVatNumber.takeIf { it.isNotBlank() },
                    supplierAddress = bill.supplierAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = bill.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = bill.issueDate,
                    dueDate = bill.dueDate,
                    amount = Money.parse(bill.amount),
                    vatAmount = Money.parse(bill.vatAmount),
                    vatRate = VatRate.parse(bill.vatRate),
                    currency = parseCurrency(bill.currency, original?.bill?.currency),
                    category = bill.category,
                    description = bill.description.takeIf { it.isNotBlank() },
                    notes = bill.notes.takeIf { it.isNotBlank() },
                    paymentTerms = bill.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = bill.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            expense = editable.expense?.let { expense ->
                val base = original?.expense ?: tech.dokus.domain.model.ExtractedExpenseFields()
                base.copy(
                    merchant = expense.merchant.takeIf { it.isNotBlank() },
                    merchantAddress = expense.merchantAddress.takeIf { it.isNotBlank() },
                    merchantVatNumber = expense.merchantVatNumber.takeIf { it.isNotBlank() },
                    date = expense.date,
                    amount = Money.parse(expense.amount),
                    vatAmount = Money.parse(expense.vatAmount),
                    vatRate = VatRate.parse(expense.vatRate),
                    currency = parseCurrency(expense.currency, original?.expense?.currency),
                    category = expense.category,
                    description = expense.description.takeIf { it.isNotBlank() },
                    isDeductible = expense.isDeductible,
                    deductiblePercentage = Percentage.parse(expense.deductiblePercentage),
                    paymentMethod = expense.paymentMethod,
                    notes = expense.notes.takeIf { it.isNotBlank() },
                    receiptNumber = expense.receiptNumber.takeIf { it.isNotBlank() }
                )
            },
            receipt = editable.receipt?.let { receipt ->
                val base = original?.receipt ?: tech.dokus.domain.model.ExtractedReceiptFields()
                base.copy(
                    merchant = receipt.merchant.takeIf { it.isNotBlank() },
                    merchantAddress = receipt.merchantAddress.takeIf { it.isNotBlank() },
                    merchantVatNumber = receipt.merchantVatNumber.takeIf { it.isNotBlank() },
                    date = receipt.date,
                    amount = Money.parse(receipt.amount),
                    vatAmount = Money.parse(receipt.vatAmount),
                    vatRate = VatRate.parse(receipt.vatRate),
                    currency = parseCurrency(receipt.currency, original?.receipt?.currency),
                    category = receipt.category,
                    description = receipt.description.takeIf { it.isNotBlank() },
                    isDeductible = receipt.isDeductible,
                    deductiblePercentage = Percentage.parse(receipt.deductiblePercentage),
                    paymentMethod = receipt.paymentMethod,
                    notes = receipt.notes.takeIf { it.isNotBlank() },
                    receiptNumber = receipt.receiptNumber.takeIf { it.isNotBlank() }
                )
            },
            proForma = editable.proForma?.let { proForma ->
                val base = original?.proForma ?: tech.dokus.domain.model.ExtractedProFormaFields()
                base.copy(
                    clientName = proForma.clientName.takeIf { it.isNotBlank() },
                    clientVatNumber = proForma.clientVatNumber.takeIf { it.isNotBlank() },
                    clientEmail = proForma.clientEmail.takeIf { it.isNotBlank() },
                    clientAddress = proForma.clientAddress.takeIf { it.isNotBlank() },
                    proFormaNumber = proForma.proFormaNumber.takeIf { it.isNotBlank() },
                    issueDate = proForma.issueDate,
                    validUntil = proForma.validUntil,
                    items = proForma.items,
                    subtotalAmount = Money.parse(proForma.subtotalAmount),
                    vatAmount = Money.parse(proForma.vatAmount),
                    totalAmount = Money.parse(proForma.totalAmount),
                    currency = parseCurrency(proForma.currency, original?.proForma?.currency),
                    notes = proForma.notes.takeIf { it.isNotBlank() },
                    termsAndConditions = proForma.termsAndConditions.takeIf { it.isNotBlank() }
                )
            },
            creditNote = editable.creditNote?.let { creditNote ->
                val base = original?.creditNote ?: tech.dokus.domain.model.ExtractedCreditNoteFields()
                base.copy(
                    counterpartyName = creditNote.counterpartyName.takeIf { it.isNotBlank() },
                    counterpartyVatNumber = creditNote.counterpartyVatNumber.takeIf { it.isNotBlank() },
                    counterpartyAddress = creditNote.counterpartyAddress.takeIf { it.isNotBlank() },
                    creditNoteNumber = creditNote.creditNoteNumber.takeIf { it.isNotBlank() },
                    originalInvoiceNumber = creditNote.originalInvoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = creditNote.issueDate,
                    items = creditNote.items,
                    subtotalAmount = Money.parse(creditNote.subtotalAmount),
                    vatAmount = Money.parse(creditNote.vatAmount),
                    totalAmount = Money.parse(creditNote.totalAmount),
                    currency = parseCurrency(creditNote.currency, original?.creditNote?.currency),
                    reason = creditNote.reason.takeIf { it.isNotBlank() },
                    notes = creditNote.notes.takeIf { it.isNotBlank() }
                )
            },
            overallConfidence = original?.overallConfidence,
            fieldConfidences = original?.fieldConfidences ?: emptyMap()
        )
    }

    private fun parseCurrency(value: String?, fallback: Currency?): Currency? {
        if (value.isNullOrBlank()) return fallback
        val normalized = value.trim().uppercase()
        return Currency.fromDbValue(normalized) ?: Currency.fromDisplayOrDefault(value)
    }
}
