package tech.dokus.features.ai.models

import tech.dokus.domain.enums.CreditNoteDirection as DomainCreditNoteDirection
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteDirection as AiCreditNoteDirection

fun FinancialExtractionResult.toDraftData(): DocumentDraftData? = when (this) {
    is FinancialExtractionResult.Invoice -> InvoiceDraftData(
        invoiceNumber = data.invoiceNumber,
        issueDate = data.issueDate,
        dueDate = data.dueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
        customerName = data.customerName,
        customerVat = data.customerVat,
        customerEmail = data.customerEmail,
        iban = data.iban,
        payment = data.payment,
        notes = null,
    )

    is FinancialExtractionResult.Bill -> BillDraftData(
        supplierName = data.supplierName,
        supplierVat = data.supplierVat,
        invoiceNumber = data.invoiceNumber,
        issueDate = data.issueDate,
        dueDate = data.dueDate,
        currency = data.currency,
        subtotalAmount = null,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
        iban = data.iban,
        payment = data.payment,
        notes = null,
    )

    is FinancialExtractionResult.CreditNote -> CreditNoteDraftData(
        creditNoteNumber = data.creditNoteNumber,
        direction = data.direction.toDomainDirection(),
        issueDate = data.issueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
        counterpartyName = data.counterpartyName,
        counterpartyVat = data.counterpartyVat,
        originalInvoiceNumber = data.originalInvoiceNumber,
        reason = data.reason,
        notes = null,
    )

    is FinancialExtractionResult.Receipt -> ReceiptDraftData(
        merchantName = data.merchantName,
        merchantVat = null,
        date = data.date,
        currency = data.currency,
        totalAmount = data.totalAmount,
        vatAmount = data.vatAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
        receiptNumber = data.receiptNumber,
        paymentMethod = data.paymentMethod,
        notes = null,
    )

    is FinancialExtractionResult.Quote,
    is FinancialExtractionResult.ProForma,
    is FinancialExtractionResult.PurchaseOrder,
    is FinancialExtractionResult.Unsupported -> null
}

private fun AiCreditNoteDirection.toDomainDirection(): DomainCreditNoteDirection = when (this) {
    AiCreditNoteDirection.SALES -> DomainCreditNoteDirection.Sales
    AiCreditNoteDirection.PURCHASE -> DomainCreditNoteDirection.Purchase
    AiCreditNoteDirection.UNKNOWN -> DomainCreditNoteDirection.Unknown
}
