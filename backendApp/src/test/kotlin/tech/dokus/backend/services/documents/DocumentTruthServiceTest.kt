package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentBlobRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewRepository
import tech.dokus.database.repository.cashflow.DocumentMatchReviewSummary
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.DocumentSourceSummary
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentMatchReviewStatus
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.foundation.backend.storage.DocumentStorageService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentTruthServiceTest {

    private val storageService = mockk<DocumentStorageService>()
    private val documentRepository = mockk<DocumentRepository>(relaxed = true)
    private val ingestionRepository = mockk<DocumentIngestionRunRepository>(relaxed = true)
    private val draftRepository = mockk<DocumentDraftRepository>(relaxed = true)
    private val blobRepository = mockk<DocumentBlobRepository>(relaxed = true)
    private val sourceRepository = mockk<DocumentSourceRepository>(relaxed = true)
    private val matchReviewRepository = mockk<DocumentMatchReviewRepository>(relaxed = true)

    private lateinit var service: DocumentTruthService

    private val tenantId = TenantId.parse("11111111-1111-1111-1111-111111111111")
    private val userId = UserId.parse("22222222-2222-2222-2222-222222222222")
    private val docId1 = DocumentId.parse("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    private val docId2 = DocumentId.parse("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    private val sourceId1 = DocumentSourceId.parse("cccccccc-cccc-cccc-cccc-cccccccccccc")
    private val blobId1 = DocumentBlobId.parse("dddddddd-dddd-dddd-dddd-dddddddddddd")
    private val reviewId1 = DocumentMatchReviewId.parse("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
    private val runId1 = IngestionRunId.parse("ffffffff-ffff-ffff-ffff-ffffffffffff")
    private val now = LocalDateTime.parse("2026-01-15T10:00:00")

    @BeforeEach
    fun setup() {
        service = DocumentTruthService(
            storageService = storageService,
            documentRepository = documentRepository,
            ingestionRepository = ingestionRepository,
            draftRepository = draftRepository,
            blobRepository = blobRepository,
            sourceRepository = sourceRepository,
            matchReviewRepository = matchReviewRepository,
            enableFuzzyMatching = false
        )
    }

    // -- resolveMatchReview SAME --

    @Test
    fun `resolveMatchReview SAME reassigns source and deletes orphan`() = runBlocking {
        val review = reviewSummary(documentId = docId2, incomingSourceId = sourceId1)
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { matchReviewRepository.getById(tenantId, reviewId1) } returns review
        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery { sourceRepository.countLinkedSources(tenantId, docId2) } returns 2
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.resolveMatchReview(tenantId, userId, reviewId1, DocumentMatchResolutionDecision.SAME)

        assertNotNull(result)
        assertEquals(DocumentIntakeOutcome.LinkedToExisting, result.outcome)
        assertEquals(docId2, result.documentId)
        assertEquals(2, result.sourceCount)

        coVerify {
            matchReviewRepository.resolve(tenantId, reviewId1, DocumentMatchReviewStatus.ResolvedSame, userId)
        }
        coVerify {
            sourceRepository.reassignToDocument(tenantId, sourceId1, docId2, DocumentSourceStatus.Linked, any())
        }
        // Orphan document should be deleted since it has 0 remaining sources
        coVerify { documentRepository.delete(tenantId, docId1) }
    }

    @Test
    fun `resolveMatchReview SAME does not delete non-orphan document`() = runBlocking {
        val review = reviewSummary(documentId = docId2, incomingSourceId = sourceId1)
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { matchReviewRepository.getById(tenantId, reviewId1) } returns review
        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery { sourceRepository.countLinkedSources(tenantId, docId2) } returns 2
        // Original document still has other sources
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 1

        val result = service.resolveMatchReview(tenantId, userId, reviewId1, DocumentMatchResolutionDecision.SAME)

        assertNotNull(result)
        coVerify(exactly = 0) { documentRepository.delete(any(), any()) }
    }

    // -- resolveMatchReview DIFFERENT --

    @Test
    fun `resolveMatchReview DIFFERENT creates new document and reassigns source`() = runBlocking {
        val review = reviewSummary(documentId = docId2, incomingSourceId = sourceId1)
        val source = sourceSummary(id = sourceId1, documentId = docId1)
        val newDocId = DocumentId.parse("99999999-9999-9999-9999-999999999999")

        coEvery { matchReviewRepository.getById(tenantId, reviewId1) } returns review
        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery { documentRepository.create(tenantId, any()) } returns newDocId
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.resolveMatchReview(tenantId, userId, reviewId1, DocumentMatchResolutionDecision.DIFFERENT)

        assertNotNull(result)
        assertEquals(DocumentIntakeOutcome.NewDocument, result.outcome)
        assertEquals(newDocId, result.documentId)

        coVerify {
            matchReviewRepository.resolve(tenantId, reviewId1, DocumentMatchReviewStatus.ResolvedDifferent, userId)
        }
        coVerify { documentRepository.create(tenantId, any()) }
        coVerify {
            sourceRepository.reassignToDocument(tenantId, sourceId1, newDocId, DocumentSourceStatus.Linked, null)
        }
    }

    @Test
    fun `resolveMatchReview returns null for unknown review`() = runBlocking {
        coEvery { matchReviewRepository.getById(tenantId, reviewId1) } returns null

        val result = service.resolveMatchReview(tenantId, userId, reviewId1, DocumentMatchResolutionDecision.SAME)

        assertNull(result)
    }

    // -- applyPostExtractionMatching --

    @Test
    fun `applyPostExtractionMatching links to existing on content hash match`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery {
            sourceRepository.findLinkedDocumentByContentHash(tenantId, any(), excludeSourceId = sourceId1)
        } returns docId2
        coEvery { sourceRepository.countLinkedSources(tenantId, docId2) } returns 2
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = docId1,
            sourceId = sourceId1,
            draftData = simpleInvoiceDraft(),
            extractedSnapshotJson = "{}"
        )

        assertEquals(DocumentIntakeOutcome.LinkedToExisting, result.outcome)
        assertEquals(docId2, result.documentId)
        assertEquals(DocumentMatchType.SameContent, result.matchType)
    }

    @Test
    fun `applyPostExtractionMatching creates review on identity match with material conflict`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)
        val existingDraft = simpleInvoiceDraft(totalMinor = 10000L) // 100.00
        val incomingDraft = simpleInvoiceDraft(totalMinor = 20000L) // 200.00

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery {
            sourceRepository.findLinkedDocumentByContentHash(tenantId, any(), excludeSourceId = sourceId1)
        } returns null
        coEvery {
            sourceRepository.findLinkedDocumentByIdentityKeyHash(tenantId, any(), excludeDocumentId = docId1)
        } returns docId2
        coEvery {
            draftRepository.getByDocumentId(docId2, tenantId)
        } returns draftSummary(extractedData = existingDraft)
        coEvery {
            matchReviewRepository.createPending(tenantId, docId2, sourceId1, any(), any(), any())
        } returns reviewId1
        coEvery { sourceRepository.countLinkedSources(tenantId, docId2) } returns 1
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = docId1,
            sourceId = sourceId1,
            draftData = incomingDraft,
            extractedSnapshotJson = "{}"
        )

        assertEquals(DocumentIntakeOutcome.PendingMatchReview, result.outcome)
        assertNotNull(result.reviewId)
    }

    @Test
    fun `applyPostExtractionMatching auto-links on identity match without conflict`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)
        val sharedDraft = simpleInvoiceDraft(totalMinor = 10000L)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery {
            sourceRepository.findLinkedDocumentByContentHash(tenantId, any(), excludeSourceId = sourceId1)
        } returns null
        coEvery {
            sourceRepository.findLinkedDocumentByIdentityKeyHash(tenantId, any(), excludeDocumentId = docId1)
        } returns docId2
        coEvery {
            draftRepository.getByDocumentId(docId2, tenantId)
        } returns draftSummary(extractedData = sharedDraft)
        coEvery { sourceRepository.countLinkedSources(tenantId, docId2) } returns 2
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = docId1,
            sourceId = sourceId1,
            draftData = sharedDraft,
            extractedSnapshotJson = "{}"
        )

        assertEquals(DocumentIntakeOutcome.LinkedToExisting, result.outcome)
        assertEquals(docId2, result.documentId)
        assertEquals(DocumentMatchType.SameDocument, result.matchType)
    }

    @Test
    fun `applyPostExtractionMatching returns NewDocument when no matches found`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery {
            sourceRepository.findLinkedDocumentByContentHash(tenantId, any(), excludeSourceId = sourceId1)
        } returns null
        coEvery {
            sourceRepository.findLinkedDocumentByIdentityKeyHash(tenantId, any(), excludeDocumentId = docId1)
        } returns null
        coEvery { sourceRepository.countLinkedSources(tenantId, docId1) } returns 1

        val result = service.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = docId1,
            sourceId = sourceId1,
            draftData = simpleInvoiceDraft(),
            extractedSnapshotJson = "{}"
        )

        assertEquals(DocumentIntakeOutcome.NewDocument, result.outcome)
        assertEquals(docId1, result.documentId)
    }

    @Test
    fun `applyPostExtractionMatching skips fuzzy matching when disabled`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery {
            sourceRepository.findLinkedDocumentByContentHash(tenantId, any(), excludeSourceId = sourceId1)
        } returns null
        coEvery {
            sourceRepository.findLinkedDocumentByIdentityKeyHash(tenantId, any(), excludeDocumentId = docId1)
        } returns null
        coEvery { sourceRepository.countLinkedSources(tenantId, docId1) } returns 1

        service.applyPostExtractionMatching(
            tenantId = tenantId,
            documentId = docId1,
            sourceId = sourceId1,
            draftData = simpleInvoiceDraft(),
            extractedSnapshotJson = "{}"
        )

        // Fuzzy matching should never be called when disabled
        coVerify(exactly = 0) {
            sourceRepository.findFuzzyCandidates(any(), any(), any(), any(), any(), any(), any())
        }
    }

    // -- deleteSource --

    @Test
    fun `deleteSource requires confirmation for last source on confirmed document`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery { sourceRepository.countLinkedSources(tenantId, docId1) } returns 1
        coEvery { draftRepository.getByDocumentId(docId1, tenantId) } returns draftSummary(
            documentStatus = DocumentStatus.Confirmed
        )

        val result = service.deleteSource(tenantId, docId1, sourceId1, confirmLastOnConfirmed = false)

        assertEquals(false, result.deleted)
        assertEquals(true, result.requiresConfirmation)
        coVerify(exactly = 0) { sourceRepository.deleteById(any(), any()) }
    }

    @Test
    fun `deleteSource cascades document delete when no sources remain on unconfirmed doc`() = runBlocking {
        val source = sourceSummary(id = sourceId1, documentId = docId1)

        coEvery { sourceRepository.getById(tenantId, sourceId1) } returns source
        coEvery { sourceRepository.countLinkedSources(tenantId, docId1) } returns 1
        coEvery { draftRepository.getByDocumentId(docId1, tenantId) } returns draftSummary(
            documentStatus = DocumentStatus.NeedsReview
        )
        coEvery { sourceRepository.deleteById(tenantId, sourceId1) } returns true
        coEvery { sourceRepository.countSources(tenantId, docId1, includeDetached = true) } returns 0

        val result = service.deleteSource(tenantId, docId1, sourceId1, confirmLastOnConfirmed = false)

        assertEquals(true, result.deleted)
        assertEquals(true, result.cascadedDocumentDelete)
        coVerify { documentRepository.delete(tenantId, docId1) }
    }

    // -- Helpers --

    private fun sourceSummary(
        id: DocumentSourceId = sourceId1,
        documentId: DocumentId = docId1,
        sourceChannel: DocumentSource = DocumentSource.Upload,
        status: DocumentSourceStatus = DocumentSourceStatus.Linked,
        matchType: DocumentMatchType? = null,
        extractedSnapshotJson: String? = null,
        identityKeyHash: String? = null,
        contentHash: String? = null
    ) = DocumentSourceSummary(
        id = id,
        tenantId = tenantId,
        documentId = documentId,
        blobId = blobId1,
        sourceChannel = sourceChannel,
        arrivalAt = now,
        contentHash = contentHash,
        identityKeyHash = identityKeyHash,
        status = status,
        matchType = matchType,
        isCorrective = false,
        extractedSnapshotJson = extractedSnapshotJson,
        detachedAt = null,
        normalizedSupplierVat = null,
        normalizedDocumentNumber = null,
        documentType = null,
        direction = null,
        filename = "test.pdf",
        inputHash = "abc123",
        storageKey = "uploads/test.pdf",
        contentType = "application/pdf",
        sizeBytes = 1024
    )

    private fun reviewSummary(
        documentId: DocumentId = docId2,
        incomingSourceId: DocumentSourceId = sourceId1
    ) = DocumentMatchReviewSummary(
        id = reviewId1,
        tenantId = tenantId,
        documentId = documentId,
        incomingSourceId = incomingSourceId,
        reasonType = DocumentMatchReviewReasonType.MaterialConflict,
        aiSummary = "Test conflict",
        aiConfidence = 0.8,
        status = DocumentMatchReviewStatus.Pending,
        resolvedBy = null,
        resolvedAt = null,
        createdAt = now,
        updatedAt = now
    )

    private fun draftSummary(
        extractedData: InvoiceDraftData? = null,
        documentStatus: DocumentStatus = DocumentStatus.NeedsReview
    ) = DraftSummary(
        documentId = docId2,
        tenantId = tenantId,
        documentStatus = documentStatus,
        documentType = DocumentType.Invoice,
        extractedData = extractedData,
        aiDraftData = null,
        aiDescription = null,
        aiKeywords = emptyList(),
        aiDraftSourceRunId = null,
        draftVersion = 1,
        draftEditedAt = null,
        draftEditedBy = null,
        contactSuggestions = emptyList(),
        counterpartySnapshot = null,
        matchEvidence = null,
        linkedContactId = null,
        linkedContactSource = null,
        counterpartyIntent = CounterpartyIntent.None,
        rejectReason = null,
        lastSuccessfulRunId = null,
        createdAt = now,
        updatedAt = now
    )

    private fun simpleInvoiceDraft(
        totalMinor: Long = 10000L,
        invoiceNumber: String = "INV-2026-001"
    ) = InvoiceDraftData(
        direction = DocumentDirection.Inbound,
        invoiceNumber = invoiceNumber,
        totalAmount = Money(totalMinor),
        seller = PartyDraft(
            name = "Supplier Inc",
            vat = VatNumber.from("BE0777887045")
        ),
        buyer = PartyDraft(
            name = "Buyer Corp",
            vat = VatNumber.from("IE9700053D")
        )
    )
}
