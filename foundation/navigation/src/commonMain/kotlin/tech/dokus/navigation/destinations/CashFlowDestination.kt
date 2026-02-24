package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface CashFlowDestination : NavigationDestination {
    @Serializable
    enum class DocumentReviewSourceFilter {
        All,
        NeedsAttention,
        Confirmed;

        val token: String
            get() = name

        companion object {
            fun fromToken(token: String?): DocumentReviewSourceFilter? {
                if (token.isNullOrBlank()) return null
                return entries.firstOrNull { it.name.equals(token, ignoreCase = true) }
            }
        }
    }

    @Serializable
    enum class DocumentReviewSourceSort {
        NewestFirst;

        val token: String
            get() = name

        companion object {
            fun fromToken(token: String?): DocumentReviewSourceSort {
                if (token.isNullOrBlank()) return NewestFirst
                return entries.firstOrNull { it.name.equals(token, ignoreCase = true) } ?: NewestFirst
            }
        }
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
     */
    @Serializable
    @SerialName("cashflow/document_review")
    data class DocumentReview(
        val documentId: String,
        val sourceFilter: String? = null,
        val sourceSort: String = DocumentReviewSourceSort.NewestFirst.token,
    ) : CashFlowDestination

    /**
     * Source evidence viewer screen for a specific document source.
     * @param documentId The parent document ID (UUID string)
     * @param sourceId The source ID (UUID string)
     */
    @Serializable
    @SerialName("cashflow/document_source_viewer")
    data class DocumentSourceViewer(
        val documentId: String,
        val sourceId: String,
    ) : CashFlowDestination

    /**
     * Single-document chat screen for RAG-powered Q&A about a specific document.
     * @param documentId The document ID (UUID string)
     */
    @Serializable
    @SerialName("cashflow/document_chat")
    data class DocumentChat(val documentId: String) : CashFlowDestination

    /**
     * Cashflow ledger screen with optional entry highlighting.
     * Used for deep linking to a specific cashflow entry.
     * @param highlightEntryId The cashflow entry ID to highlight (UUID string), or null
     */
    @Serializable
    @SerialName("cashflow/ledger")
    data class CashflowLedger(val highlightEntryId: String? = null) : CashFlowDestination
}
