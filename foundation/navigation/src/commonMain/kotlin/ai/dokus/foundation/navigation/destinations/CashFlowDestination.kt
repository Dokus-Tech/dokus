package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface CashFlowDestination : NavigationDestination {
    @Serializable
    @SerialName("cashflow/add_document")
    data object AddDocument : CashFlowDestination

    @Serializable
    @SerialName("cashflow/create_invoice")
    data object CreateInvoice : CashFlowDestination

    /**
     * Document review screen for reviewing and editing AI-extracted data.
     * @param processingId The document processing ID (UUID string)
     */
    @Serializable
    @SerialName("cashflow/document_review")
    data class DocumentReview(val processingId: String) : CashFlowDestination

    /**
     * Single-document chat screen for RAG-powered Q&A about a specific document.
     * @param processingId The document processing ID (UUID string)
     */
    @Serializable
    @SerialName("cashflow/document_chat")
    data class DocumentChat(val processingId: String) : CashFlowDestination
}