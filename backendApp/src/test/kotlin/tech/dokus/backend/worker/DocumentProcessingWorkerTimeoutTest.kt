package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.documents.postextraction.PostExtractionOrchestrator
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.queue.LlmModelSlot
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.foundation.backend.config.ProcessorConfig
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DocumentProcessingWorkerTimeoutTest {

    @Test
    fun `timed out run does not block other runs in same batch`() = runBlocking {
        val ingestionRepository = mockk<ProcessorIngestionRepository>()
        val processingAgent = mockk<DocumentProcessingAgent>()
        val documentRepository = mockk<DocumentRepository>(relaxed = true)
        val documentSsePublisher = mockk<DocumentSsePublisher>(relaxed = true)
        val tenantRepository = mockk<TenantRepository>()
        val userRepository = mockk<UserRepository>()
        val postExtractionOrchestrator = mockk<PostExtractionOrchestrator>(relaxed = true)

        val firstRun = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
        )
        val secondRun = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
        )

        coEvery { ingestionRepository.recoverStaleRunsDetailed() } returns emptyList()
        coEvery { ingestionRepository.findPendingForProcessing(2) } returns listOf(firstRun, secondRun)
        coEvery { ingestionRepository.markAsProcessing(any(), "koog-graph") } returns true
        coEvery { ingestionRepository.markAsFailed(any(), any()) } returns true
        coEvery { processingAgent.process(any()) } throws AssertionError("processingAgent must not be invoked")
        coEvery { tenantRepository.findById(firstRun.tenantId) } coAnswers {
            delay(5.seconds)
            null
        }
        coEvery { tenantRepository.findById(secondRun.tenantId) } returns null
        coEvery { userRepository.listByTenant(any(), activeOnly = true) } returns emptyList()

        val llmQueue = LlmQueue {
            slot(LlmModelSlot.Vision) { concurrency = 1 }
            slot(LlmModelSlot.Text) { concurrency = 1 }
        }.also { it.start() }
        val worker = DocumentProcessingWorker(
            ingestionRepository = ingestionRepository,
            processingAgent = processingAgent,
            documentSsePublisher = documentSsePublisher,
            config = ProcessorConfig(
                pollingInterval = 1_000,
                maxAttempts = 3,
                batchSize = 2,
            ),
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            llmQueue = llmQueue,
            postExtractionOrchestrator = postExtractionOrchestrator,
            draftRepository = mockk(relaxed = true),
        )

        try {
            withTimeout(2.seconds) {
                worker.processBatchForTest(timeout = 75.milliseconds)
            }
        } finally {
            llmQueue.stop()
        }

        coVerify(exactly = 2) { ingestionRepository.markAsProcessing(any(), "koog-graph") }
        coVerify {
            ingestionRepository.markAsFailed(
                firstRun.runId.toString(),
                match { it.contains("timed out") }
            )
        }
        coVerify {
            ingestionRepository.markAsFailed(
                secondRun.runId.toString(),
                match { it.contains("Tenant not found") }
            )
        }
    }
}
