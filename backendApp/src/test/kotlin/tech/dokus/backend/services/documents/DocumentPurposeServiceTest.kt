package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentPurposeTemplateRepository
import tech.dokus.database.entity.DocumentPurposeTemplateEntity
import tech.dokus.database.entity.DraftSummaryEntity
import tech.dokus.domain.Money
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.PartyDraftDto
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.models.PurposeEnrichmentResult

class DocumentPurposeServiceTest {

    private val documentRepository = mockk<DocumentRepository>(relaxed = true)
    private val templateRepository = mockk<DocumentPurposeTemplateRepository>(relaxed = true)
    private val similarityService = mockk<DocumentPurposeSimilarityService>(relaxed = true)
    private val processingAgent = mockk<DocumentProcessingAgent>(relaxed = true)
    private val draftRepository = mockk<tech.dokus.database.repository.drafts.DraftRepository>(relaxed = true)

    private val service = DocumentPurposeService(
        documentRepository = documentRepository,
        templateRepository = templateRepository,
        similarityService = similarityService,
        processingAgent = processingAgent,
        draftRepository = draftRepository
    )

    private val tenantId = TenantId.parse("11111111-1111-1111-1111-111111111111")
    private val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")
    private val contactId = ContactId.parse("33333333-3333-3333-3333-333333333333")

    @Test
    fun `locked purpose is never overwritten`() = runTest {
        coEvery { documentRepository.updatePurposeContext(any(), any(), any(), any()) } returns true

        service.enrichAfterContactResolution(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(),
            linkedContactId = contactId,
            currentDraft = draftSummary(
                purposeLocked = true,
                purposeBase = "Model Y leasing"
            )
        )

        coVerify(exactly = 0) { processingAgent.enrichPurpose(any()) }
        coVerify(exactly = 0) { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `template hit bypasses similarity retrieval`() = runTest {
        coEvery { documentRepository.updatePurposeContext(any(), any(), any(), any()) } returns true
        coEvery {
            templateRepository.findByCounterparty(
                tenantId = tenantId,
                counterpartyKey = "contact:$contactId",
                documentType = DocumentType.Invoice
            )
        } returns DocumentPurposeTemplateEntity(
            tenantId = tenantId,
            counterpartyKey = "contact:$contactId",
            documentType = DocumentType.Invoice,
            purposeBase = "Model Y leasing",
            periodMode = PurposePeriodMode.ServicePeriod,
            confidence = 1.0,
            usageCount = 5
        )
        coEvery { processingAgent.enrichPurpose(any()) } returns PurposeEnrichmentResult(
            purposeBase = "Model Y leasing",
            purposePeriodYear = 2026,
            purposePeriodMonth = 2,
            purposeRendered = "KBC - Model Y leasing February 2026",
            purposeSource = DocumentPurposeSource.AiTemplate,
            purposePeriodMode = PurposePeriodMode.ServicePeriod
        )
        coEvery { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        service.enrichAfterContactResolution(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(notes = "Service period January-February 2026"),
            linkedContactId = contactId,
            currentDraft = draftSummary()
        )

        coVerify(exactly = 0) { similarityService.findCandidates(any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 1) { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `counterparty fallback never uses merchant fallback when counterparty exists`() = runTest {
        coEvery { documentRepository.updatePurposeContext(any(), any(), any(), any()) } returns true
        coEvery { templateRepository.findByCounterparty(any(), any(), any()) } returns null
        coEvery {
            similarityService.findCandidates(
                tenantId = tenantId,
                documentType = DocumentType.Invoice,
                counterpartyKey = "contact:$contactId",
                merchantToken = "kbc",
                queryPurposeBase = any(),
                minSimilarity = 0.78f,
                topK = 3
            )
        } returns emptyList()
        coEvery {
            documentRepository.listConfirmedPurposeBasesByCounterparty(
                tenantId = tenantId,
                counterpartyKey = "contact:$contactId",
                documentType = DocumentType.Invoice,
                limit = 5
            )
        } returns listOf("Model Y leasing")
        coEvery { processingAgent.enrichPurpose(any()) } returns PurposeEnrichmentResult(
            purposeBase = "Model Y leasing",
            purposePeriodYear = 2026,
            purposePeriodMonth = 2,
            purposeRendered = "KBC - Model Y leasing February 2026",
            purposeSource = DocumentPurposeSource.AiRag,
            purposePeriodMode = PurposePeriodMode.IssueMonth
        )
        coEvery { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        service.enrichAfterContactResolution(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(notes = "Leasing 2026-02"),
            linkedContactId = contactId,
            currentDraft = draftSummary(counterpartyKey = "contact:$contactId", merchantToken = "kbc")
        )

        coVerify(exactly = 1) { documentRepository.listConfirmedPurposeBasesByCounterparty(any(), any(), any(), any()) }
        coVerify(exactly = 0) { documentRepository.listConfirmedPurposeBasesByMerchantToken(any(), any(), any(), any()) }
    }

    @Test
    fun `merchant fallback is used when counterparty key is missing`() = runTest {
        coEvery { documentRepository.updatePurposeContext(any(), any(), any(), any()) } returns true
        coEvery { templateRepository.findByCounterparty(any(), any(), any()) } returns null
        coEvery {
            similarityService.findCandidates(
                tenantId = tenantId,
                documentType = DocumentType.Invoice,
                counterpartyKey = null,
                merchantToken = "openai",
                queryPurposeBase = any(),
                minSimilarity = 0.78f,
                topK = 3
            )
        } returns emptyList()
        coEvery {
            documentRepository.listConfirmedPurposeBasesByMerchantToken(
                tenantId = tenantId,
                merchantToken = "openai",
                documentType = DocumentType.Invoice,
                limit = 5
            )
        } returns listOf("ChatGPT subscription")
        coEvery { processingAgent.enrichPurpose(any()) } returns PurposeEnrichmentResult(
            purposeBase = "ChatGPT subscription",
            purposePeriodYear = 2026,
            purposePeriodMonth = 2,
            purposeRendered = "OpenAI - ChatGPT subscription February 2026",
            purposeSource = DocumentPurposeSource.AiRag,
            purposePeriodMode = PurposePeriodMode.IssueMonth
        )
        coEvery { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        service.enrichAfterContactResolution(
            tenantId = tenantId,
            documentId = documentId,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(
                supplierName = "OpenAI",
                supplierVat = null,
                notes = "Subscription 02/2026"
            ),
            linkedContactId = null,
            currentDraft = draftSummary(counterpartyKey = null, merchantToken = "openai")
        )

        coVerify(exactly = 1) { documentRepository.listConfirmedPurposeBasesByMerchantToken(any(), any(), any(), any()) }
    }

    @Test
    fun `user edit locks purpose and triggers reindex when draft is confirmed`() = runTest {
        coEvery { documentRepository.updatePurposeFields(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true
        coEvery { templateRepository.upsert(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        service.applyUserPurposeEdit(
            tenantId = tenantId,
            documentId = documentId,
            draft = draftSummary(
                documentStatus = DocumentStatus.Confirmed,
                counterpartyKey = "contact:$contactId"
            ),
            purpose = "Model Y leasing",
            purposePeriodMode = PurposePeriodMode.ServicePeriod
        )

        coVerify(exactly = 1) {
            documentRepository.updatePurposeFields(
                documentId = documentId,
                tenantId = tenantId,
                purposeBase = any(),
                purposePeriodYear = any(),
                purposePeriodMonth = any(),
                purposeRendered = any(),
                purposeSource = DocumentPurposeSource.User,
                purposeLocked = true,
                purposePeriodMode = any()
            )
        }
        coVerify(exactly = 1) { similarityService.indexConfirmedDocument(tenantId, documentId) }
    }

    private fun invoiceDraft(
        supplierName: String = "KBC",
        supplierVat: VatNumber? = VatNumber.from("BE0123456789"),
        notes: String? = null
    ): InvoiceDraftData {
        return InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            issueDate = LocalDate(2026, 2, 15),
            seller = PartyDraftDto(name = supplierName, vat = supplierVat),
            buyer = PartyDraftDto(name = "Dokus"),
            totalAmount = Money.from("100.00"),
            notes = notes
        )
    }

    private fun draftSummary(
        documentStatus: DocumentStatus = DocumentStatus.NeedsReview,
        purposeLocked: Boolean = false,
        purposeBase: String? = null,
        counterpartyKey: String? = null,
        merchantToken: String? = "kbc"
    ): DraftSummaryEntity {
        val now = LocalDateTime(2026, 2, 1, 0, 0, 0)
        return DraftSummaryEntity(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = documentStatus,
            documentType = DocumentType.Invoice,
            purposeBase = purposeBase,
            purposeLocked = purposeLocked,
            counterpartyKey = counterpartyKey,
            merchantToken = merchantToken,
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            counterparty = CounterpartyInfo.Linked(contactId = contactId, source = ContactLinkSource.User),
            rejectReason = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )
    }
}
