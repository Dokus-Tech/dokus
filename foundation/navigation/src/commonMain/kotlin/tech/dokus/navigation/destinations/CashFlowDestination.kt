package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import kotlin.uuid.ExperimentalUuidApi

sealed interface CashFlowDestination : NavigationDestination {

    /**
     * Describes which documents to show in the detail screen's contextual queue panel.
     */
    @Serializable
    sealed interface DocumentDetailQueueContext {
        /** Documents matching a list filter (from documents overview or cashflow overview). */
        @Serializable
        data class DocumentList(
            val filter: DocumentListFilter = DocumentListFilter.All,
        ) : DocumentDetailQueueContext

        /** Documents belonging to a specific contact. */
        @Serializable
        data class Contact(
            val contactId: ContactId,
        ) : DocumentDetailQueueContext

        /** Documents matching a search query. */
        @Serializable
        data class Search(val query: String) : DocumentDetailQueueContext

        /** Most recent documents (fallback when no context available). */
        @Serializable
        data object Recent : DocumentDetailQueueContext
    }

    @Serializable
    @SerialName("cashflow/add_document")
    data object AddDocument : CashFlowDestination

    @Serializable
    @SerialName("cashflow/create_invoice")
    data object CreateInvoice : CashFlowDestination

    /**
     * Document detail screen for reviewing and editing AI-extracted data.
     * @param documentId The document ID (UUID string)
     * @param queueSource Controls which documents appear in the contextual queue panel
     */
    @Serializable
    @SerialName("cashflow/document_detail")
    data class DocumentDetail(
        val documentId: String,
        val filter: String? = null,
        val contactId: String? = null,
        val query: String? = null,
    ) : CashFlowDestination {
        companion object {
            @OptIn(ExperimentalUuidApi::class)
            fun from(documentId: DocumentId, context: DocumentDetailQueueContext): DocumentDetail {
                return when (context) {
                    is DocumentDetailQueueContext.DocumentList -> DocumentDetail(
                        documentId = documentId.value.toString(),
                        filter = context.filter.name
                    )

                    is DocumentDetailQueueContext.Contact -> DocumentDetail(
                        documentId = documentId.value.toString(),
                        contactId = context.contactId.value.toString()
                    )

                    is DocumentDetailQueueContext.Search -> DocumentDetail(
                        documentId = documentId.value.toString(),
                        query = context.query
                    )

                    is DocumentDetailQueueContext.Recent -> DocumentDetail(
                        documentId = documentId.value.toString()
                    )
                }
            }
        }

        val queueSource: DocumentDetailQueueContext
            get() = when {
                filter != null -> DocumentDetailQueueContext.DocumentList(
                    DocumentListFilter.valueOf(
                        filter
                    )
                )

                contactId != null -> DocumentDetailQueueContext.Contact(ContactId.parse(contactId))
                !query.isNullOrBlank() -> DocumentDetailQueueContext.Search(query)
                else -> DocumentDetailQueueContext.Recent
            }
    }

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
     * Cashflow overview screen with optional entry highlighting.
     */
    @Serializable
    @SerialName("cashflow/overview")
    data class CashFlowOverview(val highlightEntryId: String? = null) : CashFlowDestination

    /**
     * Dialog showing a QR code for downloading the mobile application.
     */
    @Serializable
    @SerialName("cashflow/dialog/app_download_qr")
    data object AppDownloadQrDialog : CashFlowDestination
}
