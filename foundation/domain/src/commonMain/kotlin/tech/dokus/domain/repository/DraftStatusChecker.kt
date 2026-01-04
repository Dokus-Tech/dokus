package tech.dokus.domain.repository

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Interface for checking document draft confirmation status.
 *
 * Used by RAGService/ChatAgent to determine if a document is confirmed
 * and eligible for chat. Chat is only allowed for Confirmed documents.
 *
 * Implementations should check the document_drafts table for status.
 */
interface DraftStatusChecker {
    /**
     * Check if a document has been confirmed by the user.
     *
     * @param tenantId The tenant ID (REQUIRED for security)
     * @param documentId The document ID to check
     * @return true if document has a draft with status=Confirmed, false otherwise
     */
    suspend fun isConfirmed(tenantId: TenantId, documentId: DocumentId): Boolean
}
