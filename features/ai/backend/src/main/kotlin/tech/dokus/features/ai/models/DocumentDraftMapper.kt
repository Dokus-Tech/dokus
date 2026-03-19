package tech.dokus.features.ai.models

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BankStatementTransactionDraftRow
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.Money
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.toEmptyDraftData

fun DocumentAiProcessingResult.toDraftData(): DocumentDraftData? {
    if (extraction is FinancialExtractionResult.Unsupported && !classification.documentType.supported) {
        return classification.documentType.toEmptyDraftData()
    }
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
        vatAmount = data.vatAmount ?: inferZeroVat(data.lineItems, data.vatBreakdown),
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
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

    is FinancialExtractionResult.CreditNote -> CreditNoteDraftData(
        creditNoteNumber = data.creditNoteNumber,
        direction = direction,
        issueDate = data.issueDate,
        currency = data.currency,
        subtotalAmount = data.subtotalAmount,
        vatAmount = data.vatAmount ?: inferZeroVat(data.lineItems, data.vatBreakdown),
        totalAmount = data.totalAmount,
        lineItems = data.lineItems,
        vatBreakdown = data.vatBreakdown,
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

    is FinancialExtractionResult.BankStatement -> BankStatementDraftData(
        direction = DocumentDirection.Neutral,
        transactions = data.rows.map { row ->
            BankStatementTransactionDraftRow(
                transactionDate = row.transactionDate,
                signedAmount = row.signedAmount,
                counterparty = CounterpartySnapshot(
                    name = row.counterpartyName,
                    iban = row.counterpartyIban,
                ),
                communication = TransactionCommunication.from(
                    structuredCommunicationRaw = row.structuredCommunicationRaw,
                    freeCommunication = row.freeCommunication,
                ),
                descriptionRaw = row.descriptionRaw,
                rowConfidence = row.rowConfidence,
                largeAmountFlag = false,
            )
        },
        accountIban = data.accountIban,
        openingBalance = data.openingBalance,
        closingBalance = data.closingBalance,
        periodStart = data.periodStart,
        periodEnd = data.periodEnd,
        institution = PartyDraft(name = data.institutionName),
        notes = null
    )

    is FinancialExtractionResult.Quote,
    is FinancialExtractionResult.ProForma,
    is FinancialExtractionResult.PurchaseOrder,
    is FinancialExtractionResult.Unsupported -> null
}

/**
 * Infer vatAmount = 0 when all evidence points to a zero-rated/exempt/reverse-charge invoice.
 * Returns Money.ZERO when:
 * - All vatBreakdown entries have amount == 0, OR
 * - All line items have vatRate == 0, OR
 * - No VAT data at all (empty breakdown + all line items have null vatRate) — non-EU seller case.
 * Returns null if we can't determine (let it stay unknown).
 */
private fun inferZeroVat(
    lineItems: List<tech.dokus.domain.model.FinancialLineItem>,
    vatBreakdown: List<tech.dokus.domain.model.VatBreakdownEntry>,
): Money? {
    // If vatBreakdown explicitly says amount = 0 for all entries
    if (vatBreakdown.isNotEmpty() && vatBreakdown.all { it.amount == 0L }) {
        return Money.ZERO
    }
    // If all line items have vatRate = 0
    if (lineItems.isNotEmpty() && lineItems.all { it.vatRate == 0 }) {
        return Money.ZERO
    }
    // No VAT data at all: empty breakdown and all line items have null vatRate (e.g. non-EU seller)
    if (vatBreakdown.isEmpty() && lineItems.isNotEmpty() && lineItems.all { it.vatRate == null }) {
        return Money.ZERO
    }
    return null
}
