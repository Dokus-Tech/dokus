package tech.dokus.features.ai.models

import tech.dokus.domain.enums.CreditNoteDirection as DomainCreditNoteDirection
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteDirection as AiCreditNoteDirection

fun FinancialExtractionResult.toDraftData(): DocumentDraftData? = when (this) {
    is FinancialExtractionResult.Invoice -> InvoiceDraftData(
        direction = DocumentDirection.Unknown,
        invoiceNumber = data.invoiceNumber,
        issueDate = data.issueDate,
        dueDate = data.dueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount,
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
        // Legacy customer fields stay populated from buyer facts.
        customerName = data.buyerName,
        customerVat = data.buyerVat,
        customerEmail = data.buyerEmail,
        iban = data.iban,
        payment = data.payment,
        notes = null,
        seller = PartyDraft(
            name = data.sellerName,
            vat = data.sellerVat,
            email = data.sellerEmail,
            streetLine1 = data.sellerStreet,
            postalCode = data.sellerPostalCode,
            city = data.sellerCity,
            country = data.sellerCountry,
        ),
        buyer = PartyDraft(
            name = data.buyerName,
            vat = data.buyerVat,
            email = data.buyerEmail,
            streetLine1 = data.buyerStreet,
            postalCode = data.buyerPostalCode,
            city = data.buyerCity,
            country = data.buyerCountry,
        ),
    )

    is FinancialExtractionResult.Bill -> BillDraftData(
        direction = DocumentDirection.Inbound,
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
        seller = PartyDraft(
            name = data.supplierName,
            vat = data.supplierVat,
        ),
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
        direction = DocumentDirection.Inbound,
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
