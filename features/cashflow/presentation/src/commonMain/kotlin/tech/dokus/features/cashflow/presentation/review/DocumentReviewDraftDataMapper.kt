package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.Email
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CanonicalPayment
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

internal class DocumentReviewDraftDataMapper {
    fun buildDraftDataFromEditable(
        editable: EditableExtractedData,
        original: DocumentDraftData?
    ): DocumentDraftData? {
        return when (editable.documentType) {
            DocumentType.Invoice -> {
                val fields = editable.invoice ?: return original
                val base = original as? InvoiceDraftData ?: InvoiceDraftData()
                base.copy(
                    customerName = fields.customerName.takeIf { it.isNotBlank() },
                    customerVat = VatNumber.from(fields.customerVatNumber),
                    customerEmail = Email.from(fields.customerEmail),
                    invoiceNumber = fields.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate,
                    dueDate = fields.dueDate,
                    currency = parseCurrency(fields.currency, base.currency),
                    subtotalAmount = Money.from(fields.subtotalAmount),
                    vatAmount = Money.from(fields.vatAmount),
                    totalAmount = Money.from(fields.totalAmount),
                    lineItems = fields.lineItems,
                    vatBreakdown = fields.vatBreakdown,
                    iban = Iban.from(fields.iban),
                    payment = CanonicalPayment.from(fields.paymentReference),
                    notes = fields.notes.takeIf { it.isNotBlank() },
                )
            }
            DocumentType.Bill -> {
                val fields = editable.bill ?: return original
                val base = original as? BillDraftData ?: BillDraftData()
                base.copy(
                    supplierName = fields.supplierName.takeIf { it.isNotBlank() },
                    supplierVat = VatNumber.from(fields.supplierVatNumber),
                    invoiceNumber = fields.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate,
                    dueDate = fields.dueDate,
                    currency = parseCurrency(fields.currency, base.currency),
                    subtotalAmount = base.subtotalAmount,
                    vatAmount = Money.from(fields.vatAmount),
                    totalAmount = Money.from(fields.totalAmount),
                    lineItems = fields.lineItems,
                    vatBreakdown = fields.vatBreakdown,
                    iban = Iban.from(fields.iban),
                    payment = CanonicalPayment.from(fields.paymentReference),
                    notes = fields.notes.takeIf { it.isNotBlank() },
                )
            }
            DocumentType.Receipt -> {
                val fields = editable.receipt ?: return original
                val base = original as? ReceiptDraftData ?: ReceiptDraftData()
                base.copy(
                    merchantName = fields.merchantName.takeIf { it.isNotBlank() },
                    merchantVat = VatNumber.from(fields.merchantVatNumber),
                    date = fields.date,
                    currency = parseCurrency(fields.currency, base.currency),
                    totalAmount = Money.from(fields.totalAmount),
                    vatAmount = Money.from(fields.vatAmount),
                    lineItems = fields.lineItems,
                    vatBreakdown = fields.vatBreakdown,
                    receiptNumber = fields.receiptNumber.takeIf { it.isNotBlank() },
                    paymentMethod = fields.paymentMethod,
                    notes = fields.notes.takeIf { it.isNotBlank() },
                )
            }
            DocumentType.CreditNote -> {
                val fields = editable.creditNote ?: return original
                val base = original as? CreditNoteDraftData ?: CreditNoteDraftData()
                base.copy(
                    creditNoteNumber = fields.creditNoteNumber.takeIf { it.isNotBlank() },
                    issueDate = fields.issueDate,
                    currency = parseCurrency(fields.currency, base.currency),
                    subtotalAmount = Money.from(fields.subtotalAmount),
                    vatAmount = Money.from(fields.vatAmount),
                    totalAmount = Money.from(fields.totalAmount),
                    lineItems = fields.lineItems,
                    vatBreakdown = fields.vatBreakdown,
                    counterpartyName = fields.counterpartyName.takeIf { it.isNotBlank() },
                    counterpartyVat = VatNumber.from(fields.counterpartyVatNumber),
                    originalInvoiceNumber = fields.originalInvoiceNumber.takeIf { it.isNotBlank() },
                    reason = fields.reason.takeIf { it.isNotBlank() },
                    notes = fields.notes.takeIf { it.isNotBlank() },
                )
            }
            else -> original
        }
    }

    private fun parseCurrency(value: String?, fallback: Currency?): Currency {
        if (value.isNullOrBlank()) return fallback ?: Currency.default
        return Currency.from(value)
    }
}
