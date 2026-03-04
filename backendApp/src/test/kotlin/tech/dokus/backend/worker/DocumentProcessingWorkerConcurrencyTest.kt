package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.ContactResolutionService
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.foundation.backend.config.ProcessorConfig
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DocumentProcessingWorkerConcurrencyTest {

    @Test
    fun `maxConcurrentRuns caps parallel ingestion execution`() = runBlocking {
        val ingestionRepository = mockk<ProcessorIngestionRepository>()
        val processingAgent = mockk<DocumentProcessingAgent>()
        val contactResolutionService = mockk<ContactResolutionService>(relaxed = true)
        val draftRepository = mockk<DocumentDraftRepository>(relaxed = true)
        val documentRepository = mockk<DocumentRepository>(relaxed = true)
        val documentTruthService = mockk<DocumentTruthService>(relaxed = true)
        val autoConfirmPolicy = mockk<AutoConfirmPolicy>(relaxed = true)
        val confirmationDispatcher = mockk<DocumentConfirmationDispatcher>(relaxed = true)
        val tenantRepository = mockk<TenantRepository>()
        val userRepository = mockk<UserRepository>()

        val runs = (1..4).map { idx ->
            IngestionItemEntity(
                runId = IngestionRunId.generate(),
                documentId = DocumentId.generate(),
                tenantId = TenantId.generate(),
                storageKey = "docs/$idx.pdf",
                filename = "$idx.pdf",
                contentType = "application/pdf"
            )
        }

        val active = AtomicInteger(0)
        val maxObserved = AtomicInteger(0)

        coEvery { ingestionRepository.recoverStaleRuns() } returns 0
        coEvery { ingestionRepository.findPendingForProcessing(10) } returns runs
        coEvery { ingestionRepository.markAsProcessing(any(), "koog-graph") } returns true
        coEvery { ingestionRepository.markAsFailed(any(), any()) } returns true
        coEvery { processingAgent.process(any()) } throws AssertionError("processingAgent must not be invoked")
        coEvery { userRepository.listByTenant(any(), activeOnly = true) } returns emptyList()
        coEvery { tenantRepository.findById(any()) } coAnswers {
            val current = active.incrementAndGet()
            maxObserved.updateAndGet { previous -> maxOf(previous, current) }
            delay(500.milliseconds)
            active.decrementAndGet()
            null
        }

        val worker = DocumentProcessingWorker(
            ingestionRepository = ingestionRepository,
            processingAgent = processingAgent,
            contactResolutionService = contactResolutionService,
            documentTruthService = documentTruthService,
            draftRepository = draftRepository,
            documentRepository = documentRepository,
            autoConfirmPolicy = autoConfirmPolicy,
            confirmationDispatcher = confirmationDispatcher,
            config = ProcessorConfig(
                pollingInterval = 1_000,
                maxAttempts = 3,
                batchSize = 10,
                maxConcurrentRuns = 1
            ),
            tenantRepository = tenantRepository,
            userRepository = userRepository
        )

        worker.processBatchForTest(timeout = 5.seconds)

        assertEquals(1, maxObserved.get())
        coVerify(exactly = runs.size) { ingestionRepository.markAsProcessing(any(), "koog-graph") }
    }
}
