package tech.dokus.backend.services.documents

import tech.dokus.backend.worker.handlers.RAGPipelineHandler
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.from
import tech.dokus.features.ai.services.DraftDataTextRenderer
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Orchestrates RAG indexing for confirmed documents.
 *
 * Generates readable text from [DocumentDraftData], then delegates to
 * [RAGPipelineHandler] for chunking, embedding, and storage.
 *
 * Called after document confirmation (both manual and auto-confirm).
 * Best-effort — failures are logged but do not block the confirmation flow.
 */
class RAGIndexingService(
    private val ragPipelineHandler: RAGPipelineHandler,
    private val documentRepository: DocumentRepository,
    private val draftRepository: DraftRepository,
) {
    private val logger = loggerFor()

    /**
     * Index a confirmed document for RAG search.
     * Generates text from draft data, chunks it, and stores embeddings.
     *
     * Safe to call multiple times — uses content-hash deduplication.
     * On re-confirmation, old chunks are replaced if content changed.
     */
    suspend fun indexDocument(tenantId: TenantId, documentId: DocumentId) {
        if (!ragPipelineHandler.isEnabled) {
            logger.debug("RAG pipeline not enabled, skipping indexing for {}", documentId)
            return
        }

        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        if (draft == null) {
            logger.warn("No draft found for document {}, skipping RAG indexing", documentId)
            return
        }

        val docDto = draftRepository.getDraftAsDocDto(tenantId, documentId, draft.documentType)
        val draftData = docDto?.let { DocumentDraftData.from(it) }
        if (draftData == null) {
            logger.debug("No extracted data for document {}, skipping RAG indexing", documentId)
            return
        }

        val text = DraftDataTextRenderer.render(
            draft = draftData,
            counterpartyName = draft.counterpartyDisplayName,
        )

        if (text.isBlank()) {
            logger.debug("Empty text for document {}, skipping RAG indexing", documentId)
            return
        }

        logger.info("Indexing document {} for RAG ({} chars)", documentId, text.length)

        ragPipelineHandler.chunkAndEmbed(
            tenantId = tenantId.toString(),
            documentId = documentId.toString(),
            rawText = text,
        )
    }
}
