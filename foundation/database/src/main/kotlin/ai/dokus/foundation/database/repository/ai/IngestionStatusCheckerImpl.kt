package ai.dokus.foundation.database.repository.ai

import ai.dokus.ai.services.IngestionStatusChecker
import ai.dokus.foundation.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

/**
 * Implementation of IngestionStatusChecker that uses DocumentIngestionRunRepository.
 *
 * Checks if a document has an active ingestion run (Queued or Processing status).
 */
class IngestionStatusCheckerImpl(
    private val ingestionRunRepository: DocumentIngestionRunRepository
) : IngestionStatusChecker {

    override suspend fun isProcessing(tenantId: TenantId, documentId: DocumentId): Boolean {
        // findActiveRun returns the most recent active run (Queued or Processing)
        // If it exists, the document is still being processed
        return ingestionRunRepository.findActiveRun(documentId, tenantId) != null
    }
}
