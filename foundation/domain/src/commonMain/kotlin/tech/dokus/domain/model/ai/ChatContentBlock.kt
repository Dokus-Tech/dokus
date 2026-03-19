package tech.dokus.domain.model.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed content block in an AI chat response.
 *
 * When the AI produces structured data (summaries, document references, invoice breakdowns,
 * transaction references), the response contains a list of these blocks instead of plain text.
 * The frontend renders each block type with the appropriate component.
 */
@Serializable
sealed interface ChatContentBlock {

    /** Plain text segment. */
    @Serializable
    @SerialName("text")
    data class Text(val content: String) : ChatContentBlock

    /** Summary table with key-value rows (totals, counts, comparisons). */
    @Serializable
    @SerialName("summary")
    data class Summary(val rows: List<SummaryRow>) : ChatContentBlock

    /** List of referenced documents with optional bulk download. */
    @Serializable
    @SerialName("documents")
    data class Documents(
        val items: List<DocumentReference>,
        val showDownloadAll: Boolean = false,
    ) : ChatContentBlock

    /** Detailed invoice/document breakdown with line items. */
    @Serializable
    @SerialName("invoice_detail")
    data class InvoiceDetail(
        val name: String,
        val ref: String,
        val date: String,
        val lines: List<InvoiceLine>,
        val total: String,
        val documentId: String? = null,
    ) : ChatContentBlock

    /** List of referenced bank transactions. */
    @Serializable
    @SerialName("transactions")
    data class Transactions(val items: List<TransactionReference>) : ChatContentBlock
}

@Serializable
data class SummaryRow(
    val label: String,
    val value: String,
)

@Serializable
enum class DocumentReferenceType {
    @SerialName("Invoice") Invoice,
    @SerialName("Receipt") Receipt,
    @SerialName("CreditNote") CreditNote,
    @SerialName("Expense") Expense,
}

@Serializable
data class DocumentReference(
    val documentId: String? = null,
    val name: String,
    val ref: String? = null,
    val type: DocumentReferenceType,
    val amount: String? = null,
    val currency: String = "EUR",
)

@Serializable
data class InvoiceLine(
    val description: String,
    val price: String,
    val vatRate: String? = null,
)

@Serializable
enum class TransactionStatus {
    @SerialName("unmatched") Unmatched,
    @SerialName("review") Review,
    @SerialName("matched") Matched,
}

@Serializable
data class TransactionReference(
    val description: String,
    val amount: String,
    val date: String? = null,
    val status: TransactionStatus,
    val transactionId: String? = null,
)
