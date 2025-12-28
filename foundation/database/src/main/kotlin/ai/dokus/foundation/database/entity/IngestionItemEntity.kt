package ai.dokus.foundation.database.entity

/**
 * Item representing a document ready for processing.
 * Contains all info needed by the worker to process a document.
 */
data class IngestionItemEntity(
    val runId: String,
    val documentId: String,
    val tenantId: String,
    val storageKey: String,
    val filename: String,
    val contentType: String
)