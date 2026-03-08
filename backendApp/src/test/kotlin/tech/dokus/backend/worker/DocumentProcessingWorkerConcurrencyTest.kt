package tech.dokus.backend.worker

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.cashflow.BankStatementMatchingService
import tech.dokus.backend.services.cashflow.InvoiceBankAutomationService
import tech.dokus.backend.services.documents.ContactResolutionService
import tech.dokus.backend.services.documents.DocumentPurposeService
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.database.entity.IngestionItemEntity
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.foundation.backend.config.ProcessorConfig
import kotlin.time.Duration.Companion.seconds

class DocumentProcessingWorkerConcurrencyTest {

    private fun testConfig() = ProcessorConfig(
        pollingInterval = 1_000,
        maxAttempts = 3,
        batchSize = 10,
    )

    private fun testLlmQueue(): LlmQueue = LlmQueue().also { it.start() }

    @Test
    fun `run already claimed by another worker is skipped without side effects`() = runBlocking {
        val ingestionRepository = mockk<ProcessorIngestionRepository>()
        val processingAgent = mockk<DocumentProcessingAgent>()
        val contactResolutionService = mockk<ContactResolutionService>(relaxed = true)
        val purposeService = mockk<DocumentPurposeService>(relaxed = true)
        val draftRepository = mockk<DocumentDraftRepository>(relaxed = true)
        val documentTruthService = mockk<DocumentTruthService>(relaxed = true)
        val bankStatementMatchingService = mockk<BankStatementMatchingService>(relaxed = true)
        val invoiceBankAutomationService = mockk<InvoiceBankAutomationService>(relaxed = true)
        val autoConfirmPolicy = mockk<AutoConfirmPolicy>(relaxed = true)
        val confirmationDispatcher = mockk<DocumentConfirmationDispatcher>(relaxed = true)
        val documentSsePublisher = mockk<DocumentSsePublisher>(relaxed = true)
        val tenantRepository = mockk<TenantRepository>(relaxed = true)
        val userRepository = mockk<UserRepository>(relaxed = true)

        val run = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            storageKey = "docs/claimed.pdf",
            filename = "claimed.pdf",
            contentType = "application/pdf"
        )

        coEvery { ingestionRepository.recoverStaleRunsDetailed() } returns emptyList()
        coEvery { ingestionRepository.findPendingForProcessing(10) } returns listOf(run)
        coEvery { ingestionRepository.markAsProcessing(run.runId.toString(), "koog-graph") } returns false
        coEvery { ingestionRepository.markAsFailed(any(), any()) } returns true
        coEvery { processingAgent.process(any()) } throws AssertionError("processingAgent must not be invoked")

        val llmQueue = testLlmQueue()
        val worker = DocumentProcessingWorker(
            ingestionRepository = ingestionRepository,
            processingAgent = processingAgent,
            contactResolutionService = contactResolutionService,
            purposeService = purposeService,
            documentTruthService = documentTruthService,
            draftRepository = draftRepository,
            bankStatementMatchingService = bankStatementMatchingService,
            invoiceBankAutomationService = invoiceBankAutomationService,
            autoConfirmPolicy = autoConfirmPolicy,
            confirmationDispatcher = confirmationDispatcher,
            documentSsePublisher = documentSsePublisher,
            config = testConfig(),
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            llmQueue = llmQueue
        )

        try {
            worker.processBatchForTest(timeout = 5.seconds)
        } finally {
            llmQueue.stop()
        }

        coVerify(exactly = 1) { ingestionRepository.markAsProcessing(run.runId.toString(), "koog-graph") }
        coVerify(exactly = 0) { ingestionRepository.markAsFailed(any(), any()) }
        coVerify(exactly = 0) { tenantRepository.findById(any()) }
        coVerify(exactly = 0) { processingAgent.process(any()) }
    }

    @Test
    fun `missing source channel falls back to document source`() = runBlocking {
        val ingestionRepository = mockk<ProcessorIngestionRepository>()
        val processingAgent = mockk<DocumentProcessingAgent>()
        val contactResolutionService = mockk<ContactResolutionService>(relaxed = true)
        val purposeService = mockk<DocumentPurposeService>(relaxed = true)
        val draftRepository = mockk<DocumentDraftRepository>(relaxed = true)
        val documentTruthService = mockk<DocumentTruthService>(relaxed = true)
        val bankStatementMatchingService = mockk<BankStatementMatchingService>(relaxed = true)
        val invoiceBankAutomationService = mockk<InvoiceBankAutomationService>(relaxed = true)
        val autoConfirmPolicy = mockk<AutoConfirmPolicy>(relaxed = true)
        val confirmationDispatcher = mockk<DocumentConfirmationDispatcher>(relaxed = true)
        val documentSsePublisher = mockk<DocumentSsePublisher>(relaxed = true)
        val tenantRepository = mockk<TenantRepository>()
        val userRepository = mockk<UserRepository>(relaxed = true)

        val run = IngestionItemEntity(
            runId = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            sourceChannel = null,
            documentSource = DocumentSource.Email,
            storageKey = "docs/fallback.pdf",
            filename = "fallback.pdf",
            contentType = "application/pdf"
        )

        coEvery { ingestionRepository.recoverStaleRunsDetailed() } returns emptyList()
        coEvery { ingestionRepository.findPendingForProcessing(10) } returns listOf(run)
        coEvery { ingestionRepository.markAsProcessing(run.runId.toString(), "koog-graph") } returns true
        coEvery { ingestionRepository.markAsFailed(any(), any()) } returns true
        coEvery { tenantRepository.findById(run.tenantId) } returns mockk(relaxed = true)
        coEvery { userRepository.listByTenant(run.tenantId, activeOnly = true) } returns emptyList()
        coEvery { processingAgent.process(any()) } throws IllegalStateException("stop after source resolution")

        val llmQueue = testLlmQueue()
        val worker = DocumentProcessingWorker(
            ingestionRepository = ingestionRepository,
            processingAgent = processingAgent,
            contactResolutionService = contactResolutionService,
            purposeService = purposeService,
            documentTruthService = documentTruthService,
            draftRepository = draftRepository,
            bankStatementMatchingService = bankStatementMatchingService,
            invoiceBankAutomationService = invoiceBankAutomationService,
            autoConfirmPolicy = autoConfirmPolicy,
            confirmationDispatcher = confirmationDispatcher,
            documentSsePublisher = documentSsePublisher,
            config = testConfig(),
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            llmQueue = llmQueue
        )

        try {
            worker.processBatchForTest(timeout = 5.seconds)
        } finally {
            llmQueue.stop()
        }

        coVerify(exactly = 1) {
            processingAgent.process(match { input -> input.sourceChannel == DocumentSource.Email })
        }
        coVerify(exactly = 1) {
            ingestionRepository.markAsFailed(
                run.runId.toString(),
                match { it.contains("stop after source resolution") }
            )
        }
    }
}
