package tech.dokus.domain.repository

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Interface for checking document ingestion (processing) status.
 *
 * Used by RAGService to determine if a document is still being processed
 * (has active ingestion runs with Queued or Processing status).
 *
 * Implementations should check the document_ingestion_runs table
 * for active runs.
 */
interface IngestionStatusChecker {
    /**
     * Check if a document is currently being processed.
     *
     * @param tenantId The tenant ID (REQUIRED for security)
     * @param documentId The document ID to check
     * @return true if there's an active ingestion run (Queued or Processing), false otherwise
     */
    suspend fun isProcessing(tenantId: TenantId, documentId: DocumentId): Boolean
}
