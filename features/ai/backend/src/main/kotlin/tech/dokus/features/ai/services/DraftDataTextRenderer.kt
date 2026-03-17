package tech.dokus.features.ai.services

import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.toDocumentType

private const val MAX_BANK_STATEMENT_ROWS = 50

/**
 * Renders [DocumentDraftData] into human-readable text for RAG chunking.
 *
 * The rendered text is designed to be:
 * - Searchable via vector similarity (natural language, not JSON)
 * - Informative (includes all key financial fields)
 * - Compact (no redundant labels for null fields)
 */
object DraftDataTextRenderer {

    fun render(draft: DocumentDraftData, counterpartyName: String? = null): String = buildString {
        when (draft) {
            is InvoiceDraftData -> renderInvoice(draft, counterpartyName)
            is CreditNoteDraftData -> renderCreditNote(draft, counterpartyName)
            is ReceiptDraftData -> renderReceipt(draft, counterpartyName)
            is BankStatementDraftData -> renderBankStatement(draft)
            else -> renderClassifiedOnly(draft, counterpartyName)
        }
    }

    private fun StringBuilder.renderInvoice(d: InvoiceDraftData, counterpartyName: String?) {
        appendLine("Invoice")
        counterpartyName?.let { appendLine("Counterparty: $it") }
        d.seller.name?.let { appendLine("Seller: $it") }
        d.seller.vat?.let { appendLine("Seller VAT: $it") }
        d.buyer.name?.let { appendLine("Buyer: $it") }
        d.buyer.vat?.let { appendLine("Buyer VAT: $it") }
        d.invoiceNumber?.let { appendLine("Invoice Number: $it") }
        d.issueDate?.let { appendLine("Issue Date: $it") }
        d.dueDate?.let { appendLine("Due Date: $it") }
        appendLine("Direction: ${d.direction}")
        appendLine("Currency: ${d.currency}")

        if (d.lineItems.isNotEmpty()) {
            appendLine()
            appendLine("Line Items:")
            d.lineItems.forEach { item ->
                val desc = item.description ?: "Item"
                val parts = listOfNotNull(
                    item.unitPrice?.let { "unit price $it" },
                    item.quantity?.let { "qty $it" },
                    item.netAmount?.let { "net $it" },
                    item.vatRate?.let { "VAT ${it}%" },
                )
                appendLine("- $desc: ${parts.joinToString(", ")}")
            }
        }

        appendLine()
        d.subtotalAmount?.let { appendLine("Subtotal: $it") }
        d.vatAmount?.let { appendLine("VAT: $it") }
        d.totalAmount?.let { appendLine("Total: $it") }
        d.iban?.let { appendLine("IBAN: $it") }
        d.notes?.takeIf { it.isNotBlank() }?.let { appendLine("Notes: $it") }
    }

    private fun StringBuilder.renderCreditNote(d: CreditNoteDraftData, counterpartyName: String?) {
        appendLine("Credit Note")
        counterpartyName?.let { appendLine("Counterparty: $it") }
        d.seller.name?.let { appendLine("Seller: $it") }
        d.buyer.name?.let { appendLine("Buyer: $it") }
        d.creditNoteNumber?.let { appendLine("Credit Note Number: $it") }
        d.issueDate?.let { appendLine("Issue Date: $it") }
        d.originalInvoiceNumber?.let { appendLine("Original Invoice: $it") }
        appendLine("Direction: ${d.direction}")

        if (d.lineItems.isNotEmpty()) {
            appendLine()
            appendLine("Line Items:")
            d.lineItems.forEach { item ->
                val desc = item.description ?: "Item"
                val parts = listOfNotNull(
                    item.netAmount?.let { "net $it" },
                    item.vatRate?.let { "VAT ${it}%" },
                )
                appendLine("- $desc: ${parts.joinToString(", ")}")
            }
        }

        appendLine()
        d.subtotalAmount?.let { appendLine("Subtotal: $it") }
        d.vatAmount?.let { appendLine("VAT: $it") }
        d.totalAmount?.let { appendLine("Total: $it") }
        d.reason?.takeIf { it.isNotBlank() }?.let { appendLine("Reason: $it") }
        d.notes?.takeIf { it.isNotBlank() }?.let { appendLine("Notes: $it") }
    }

    private fun StringBuilder.renderReceipt(d: ReceiptDraftData, counterpartyName: String?) {
        appendLine("Receipt")
        val merchant = counterpartyName ?: d.merchantName
        merchant?.let { appendLine("Merchant: $it") }
        d.merchantVat?.let { appendLine("Merchant VAT: $it") }
        d.receiptNumber?.let { appendLine("Receipt Number: $it") }
        d.date?.let { appendLine("Date: $it") }
        d.paymentMethod?.let { appendLine("Payment Method: $it") }

        if (d.lineItems.isNotEmpty()) {
            appendLine()
            appendLine("Items:")
            d.lineItems.forEach { item ->
                val desc = item.description ?: "Item"
                val amount = item.netAmount ?: item.unitPrice
                appendLine("- $desc: $amount")
            }
        }

        appendLine()
        d.totalAmount?.let { appendLine("Total: $it") }
        d.vatAmount?.let { appendLine("VAT: $it") }
        d.notes?.takeIf { it.isNotBlank() }?.let { appendLine("Notes: $it") }
    }

    private fun StringBuilder.renderBankStatement(d: BankStatementDraftData) {
        appendLine("Bank Statement")
        d.institution.name?.let { appendLine("Institution: $it") }
        d.accountIban?.let { appendLine("Account IBAN: $it") }
        d.periodStart?.let { appendLine("Period Start: $it") }
        d.periodEnd?.let { appendLine("Period End: $it") }
        d.openingBalance?.let { appendLine("Opening Balance: $it") }
        d.closingBalance?.let { appendLine("Closing Balance: $it") }

        val rows = d.transactions.take(MAX_BANK_STATEMENT_ROWS)
        if (rows.isNotEmpty()) {
            appendLine()
            appendLine("Transactions (${d.transactions.size} total):")
            rows.forEach { row ->
                val parts = listOfNotNull(
                    row.transactionDate?.toString(),
                    row.signedAmount?.toString(),
                    row.counterparty.name,
                    row.descriptionRaw,
                )
                appendLine("- ${parts.joinToString(" | ")}")
            }
            if (d.transactions.size > MAX_BANK_STATEMENT_ROWS) {
                appendLine("... and ${d.transactions.size - MAX_BANK_STATEMENT_ROWS} more transactions")
            }
        }

        d.notes?.takeIf { it.isNotBlank() }?.let { appendLine("Notes: $it") }
    }

    private fun StringBuilder.renderClassifiedOnly(draft: DocumentDraftData, counterpartyName: String?) {
        val type = draft.toDocumentType()
        appendLine("${type.name} document")
        counterpartyName?.let { appendLine("Counterparty: $it") }
    }
}
