package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentListFilter

sealed interface CashFlowDestination : NavigationDestination {

    /**
     * Describes which documents to show in the review screen's contextual queue panel.
     */
    @Serializable
    sealed interface DocumentReviewQueueSource {
        /** Documents matching a list filter (from documents overview or cashflow ledger). */
        @Serializable
        @SerialName("list")
        data class DocumentList(
            val filter: DocumentListFilter = DocumentListFilter.All,
        ) : DocumentReviewQueueSource

        /** Documents belonging to a specific contact. */
        @Serializable
        @SerialName("contact")
        data class Contact(
            val contactId: String,
            val contactName: String,
        ) : DocumentReviewQueueSource

        /** Documents matching a search query. */
        @Serializable
        @SerialName("search")
        data class Search(val query: String) : DocumentReviewQueueSource

        /** Most recent documents (fallback when no context available). */
        @Serializable
        @SerialName("recent")
        data object Recent : DocumentReviewQueueSource
    }

    @Serializable
    @SerialName("cashflow/add_document")
    data object AddDocument : CashFlowDestination

    @Serializable
    @SerialName("cashflow/create_invoice")
    data object CreateInvoice : CashFlowDestination

    /**
     * Document review screen for reviewing and editing AI-extracted data.
     * @param documentId The document ID (UUID string)
     * @param queueSource Controls which documents appear in the contextual queue panel
     */
    @Serializable
    @SerialName("cashflow/document_review")
    data class DocumentReview(
        val documentId: String,
        val queueSource: DocumentReviewQueueSource = DocumentReviewQueueSource.Recent,
    ) : CashFlowDestination

    /**
     * Source evidence viewer screen for a specific document source.
     */
    @Serializable
    @SerialName("cashflow/document_source_viewer")
    data class DocumentSourceViewer(
        val documentId: String,
        val sourceId: String,
    ) : CashFlowDestination

    /**
     * Single-document chat screen for RAG-powered Q&A about a specific document.
     */
    @Serializable
    @SerialName("cashflow/document_chat")
    data class DocumentChat(val documentId: String) : CashFlowDestination

    /**
     * Cashflow ledger screen with optional entry highlighting.
     */
    @Serializable
    @SerialName("cashflow/ledger")
    data class CashflowLedger(val highlightEntryId: String? = null) : CashFlowDestination
}
