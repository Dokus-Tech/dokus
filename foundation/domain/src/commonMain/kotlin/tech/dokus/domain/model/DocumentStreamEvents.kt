package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId

object DocumentStreamEventNames {
    const val CollectionChanged = "documents_changed"
    const val Snapshot = "document_snapshot"
    const val Deleted = "document_deleted"
}

@Serializable
data class DocumentCollectionChangedEventDto(
    val documentId: DocumentId? = null
)

@Serializable
data class DocumentDeletedEventDto(
    val documentId: DocumentId
)

sealed interface DocumentRecordStreamEvent {
    data class Snapshot(val record: DocumentDetailDto) : DocumentRecordStreamEvent
    data object Deleted : DocumentRecordStreamEvent
}
