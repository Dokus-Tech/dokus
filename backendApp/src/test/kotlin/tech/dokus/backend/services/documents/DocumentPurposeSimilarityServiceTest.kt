package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentPurposeSimilarityRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.services.EmbeddingService
import kotlin.test.assertTrue

class DocumentPurposeSimilarityServiceTest {

    private val draftRepository = mockk<DocumentDraftRepository>()
    private val similarityRepository = mockk<DocumentPurposeSimilarityRepository>(relaxed = true)
    private val embeddingService = mockk<EmbeddingService>()

    private val service = DocumentPurposeSimilarityService(
        draftRepository = draftRepository,
        similarityRepository = similarityRepository,
        embeddingService = embeddingService
    )

    private val tenantId = TenantId.parse("11111111-1111-1111-1111-111111111111")
    private val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

    @Test
    fun `findCandidates degrades to empty when embedding fails`() = runBlocking {
        coEvery { embeddingService.generateEmbedding(any()) } throws RuntimeException("embedding down")

        val result = service.findCandidates(
            tenantId = tenantId,
            documentType = DocumentType.Invoice,
            counterpartyKey = "contact:1",
            merchantToken = "openai",
            queryPurposeBase = "ChatGPT subscription"
        )

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { similarityRepository.searchSimilarPurposeBases(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `findCandidates returns empty when retrieval has no matches`() = runBlocking {
        coEvery { embeddingService.generateEmbedding(any()) } returns EmbeddingService.EmbeddingResult(
            embedding = listOf(0.11f, 0.22f, 0.33f),
            dimensions = 3,
            model = "nomic"
        )
        coEvery {
            similarityRepository.searchSimilarPurposeBases(
                tenantId = tenantId,
                documentType = DocumentType.Invoice,
                counterpartyKey = "contact:1",
                merchantToken = "openai",
                queryEmbedding = any(),
                minSimilarity = any(),
                topK = any(),
                confirmedOnly = true
            )
        } returns emptyList()

        val result = service.findCandidates(
            tenantId = tenantId,
            documentType = DocumentType.Invoice,
            counterpartyKey = "contact:1",
            merchantToken = "openai",
            queryPurposeBase = "ChatGPT subscription"
        )

        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { similarityRepository.searchSimilarPurposeBases(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `indexConfirmedDocument still upserts when embedding fails`() = runBlocking {
        coEvery { draftRepository.getByDocumentId(documentId, tenantId) } returns confirmedDraft()
        coEvery { embeddingService.generateEmbedding(any()) } throws RuntimeException("embedding down")

        service.indexConfirmedDocument(tenantId = tenantId, documentId = documentId)

        coVerify(exactly = 1) {
            similarityRepository.upsertForDocument(
                tenantId = tenantId,
                documentId = documentId,
                documentType = DocumentType.Invoice,
                counterpartyKey = "contact:123",
                merchantToken = "openai",
                purposeBase = "ChatGPT subscription",
                purposeRendered = "OpenAI - ChatGPT subscription February 2026",
                purposeSource = any(),
                embedding = null,
                embeddingModel = null
            )
        }
    }

    private fun confirmedDraft(): DraftSummary {
        val now = LocalDateTime(2026, 2, 1, 0, 0, 0)
        return DraftSummary(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice,
            extractedData = null,
            aiDraftData = null,
            purposeBase = "ChatGPT subscription",
            purposeRendered = "OpenAI - ChatGPT subscription February 2026",
            purposeSource = tech.dokus.domain.enums.DocumentPurposeSource.AiRag,
            counterpartyKey = "contact:123",
            merchantToken = "openai",
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = null,
            counterpartyIntent = CounterpartyIntent.None,
            rejectReason = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )
    }
}
