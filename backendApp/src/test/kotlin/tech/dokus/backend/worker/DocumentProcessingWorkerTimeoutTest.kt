package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.ContactResolutionService
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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DocumentProcessingWorkerTimeoutTest {

    @Test
    fun `timed out run does not block other runs in same batch`() = runBlocking {
        val ingestionRepository = mockk<ProcessorIngestionRepository>()
        val processingAgent = mockk<DocumentProcessingAgent>()
        val contactResolutionService = mockk<ContactResolutionService>(relaxed = true)
        val draftRepository = mockk<DocumentDraftRepository>(relaxed = true)
        val documentRepository = mockk<DocumentRepository>(relaxed = true)
        val autoConfirmPolicy = mockk<AutoConfirmPolicy>(relaxed = true)
        val confirmationDispatcher = mockk<DocumentConfirmationDispatcher>(relaxed = true)
        val tenantRepository = mockk<TenantRepository>()
        val userRepository = mockk<UserRepository>()

        val firstRun = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            storageKey = "docs/first.pdf",
            filename = "first.pdf",
            contentType = "application/pdf"
        )
        val secondRun = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            storageKey = "docs/second.pdf",
            filename = "second.pdf",
            contentType = "application/pdf"
        )

        coEvery { ingestionRepository.recoverStaleRuns() } returns 0
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

        val worker = DocumentProcessingWorker(
            ingestionRepository = ingestionRepository,
            processingAgent = processingAgent,
            contactResolutionService = contactResolutionService,
            draftRepository = draftRepository,
            documentRepository = documentRepository,
            autoConfirmPolicy = autoConfirmPolicy,
            confirmationDispatcher = confirmationDispatcher,
            config = ProcessorConfig(
                pollingInterval = 1_000,
                maxAttempts = 3,
                batchSize = 2
            ),
            tenantRepository = tenantRepository,
            userRepository = userRepository
        )

        withTimeout(2.seconds) {
            worker.processBatchForTest(timeout = 75.milliseconds)
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
