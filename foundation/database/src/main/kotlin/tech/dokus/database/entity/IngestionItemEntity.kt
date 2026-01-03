package tech.dokus.database.entity

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
    val contentType: String,
    // Processing overrides (null = use defaults)
    val overrideMaxPages: Int? = null,
    val overrideDpi: Int? = null,
    val overrideTimeoutSeconds: Int? = null
)
