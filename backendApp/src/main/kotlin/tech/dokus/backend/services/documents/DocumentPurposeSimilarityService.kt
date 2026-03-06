package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentPurposeSimilarityRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

private const val DefaultPurposeSimilarityThreshold = 0.78f
private const val DefaultPurposeTopK = 3

class DocumentPurposeSimilarityService(
    private val draftRepository: DocumentDraftRepository,
    private val similarityRepository: DocumentPurposeSimilarityRepository,
    private val embeddingService: EmbeddingService
) {
    private val logger = loggerFor()

    @Suppress("LongParameterList")
    suspend fun findCandidates(
        tenantId: TenantId,
        documentType: DocumentType,
        counterpartyKey: String?,
        merchantToken: String?,
        queryPurposeBase: String?,
        minSimilarity: Float = DefaultPurposeSimilarityThreshold,
        topK: Int = DefaultPurposeTopK
    ): List<String> {
        val query = queryPurposeBase
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val key = counterpartyKey?.takeIf { it.isNotBlank() } ?: merchantToken?.takeIf { it.isNotBlank() }
        if (key == null) return emptyList()

        val embedding = runSuspendCatching {
            embeddingService.generateEmbedding(query)
        }.onFailure { error ->
            logger.warn("Purpose similarity embedding failed for tenant={} type={}: {}", tenantId, documentType, error.message)
        }.getOrNull() ?: return emptyList()

        return runSuspendCatching {
            similarityRepository.searchSimilarPurposeBases(
                tenantId = tenantId,
                documentType = documentType,
                counterpartyKey = counterpartyKey,
                merchantToken = merchantToken,
                queryEmbedding = embedding.embedding,
                minSimilarity = minSimilarity,
                topK = topK,
                confirmedOnly = true
            )
        }.onFailure { error ->
            logger.warn(
                "Purpose similarity retrieval failed for tenant={} type={}: {}",
                tenantId,
                documentType,
                error.message
            )
        }.getOrElse { emptyList() }
    }

    suspend fun indexConfirmedDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ) {
        val draft = draftRepository.getByDocumentId(documentId, tenantId) ?: return
        if (draft.documentStatus != DocumentStatus.Confirmed) return

        val documentType = draft.documentType ?: return
        val purposeBase = draft.purposeBase?.trim()?.takeIf { it.isNotBlank() } ?: return
        val purposeRendered = draft.purposeRendered?.trim()?.takeIf { it.isNotBlank() }

        val embedding = runSuspendCatching {
            embeddingService.generateEmbedding(purposeBase)
        }.onFailure { error ->
            logger.warn("Purpose similarity indexing embedding failed for document {}: {}", documentId, error.message)
        }.getOrNull()

        runSuspendCatching {
            similarityRepository.upsertForDocument(
                tenantId = tenantId,
                documentId = documentId,
                documentType = documentType,
                counterpartyKey = draft.counterpartyKey,
                merchantToken = draft.merchantToken,
                purposeBase = purposeBase,
                purposeRendered = purposeRendered,
                purposeSource = draft.purposeSource,
                embedding = embedding?.embedding,
                embeddingModel = embedding?.model
            )
        }.onFailure { error ->
            logger.warn("Purpose similarity indexing upsert failed for document {}: {}", documentId, error.message)
        }
    }
}
