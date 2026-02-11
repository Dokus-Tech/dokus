package tech.dokus.features.ai.models

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.ReceiptDraftData

fun DocumentAiProcessingResult.toDraftData(): DocumentDraftData? {
    return extraction.toDraftData(directionResolution.direction)
}

private fun FinancialExtractionResult.toDraftData(direction: DocumentDirection): DocumentDraftData? = when (this) {
    is FinancialExtractionResult.Invoice -> InvoiceDraftData(
        direction = direction,
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

    is FinancialExtractionResult.CreditNote -> {
        val counterpartyName = when (direction) {
            DocumentDirection.Inbound -> data.sellerName
            DocumentDirection.Outbound -> data.buyerName
            DocumentDirection.Unknown -> data.buyerName ?: data.sellerName
        }
        val counterpartyVat = when (direction) {
            DocumentDirection.Inbound -> data.sellerVat
            DocumentDirection.Outbound -> data.buyerVat
            DocumentDirection.Unknown -> data.buyerVat ?: data.sellerVat
        }

        CreditNoteDraftData(
            creditNoteNumber = data.creditNoteNumber,
            direction = direction,
            issueDate = data.issueDate,
            currency = data.currency,
            subtotalAmount = data.subtotalAmount,
            vatAmount = data.vatAmount,
            totalAmount = data.totalAmount,
            lineItems = data.lineItems,
            vatBreakdown = data.vatBreakdown,
            counterpartyName = counterpartyName,
            counterpartyVat = counterpartyVat,
            originalInvoiceNumber = data.originalInvoiceNumber,
            reason = data.reason,
            notes = null,
            seller = PartyDraft(
                name = data.sellerName,
                vat = data.sellerVat,
            ),
            buyer = PartyDraft(
                name = data.buyerName,
                vat = data.buyerVat,
            ),
        )
    }

    is FinancialExtractionResult.Receipt -> ReceiptDraftData(
        direction = direction,
        merchantName = data.merchantName,
        merchantVat = data.merchantVat,
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
