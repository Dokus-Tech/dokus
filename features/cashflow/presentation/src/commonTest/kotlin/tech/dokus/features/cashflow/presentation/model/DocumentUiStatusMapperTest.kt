package tech.dokus.features.cashflow.presentation.model

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.foundation.aura.model.DocumentUiStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DocumentUiStatusMapperTest {

    // === FAILED CASES ===

    @Test
    fun `returns Failed when ingestion status is Failed`() {
        val record = createRecord(ingestionStatus = IngestionStatus.Failed)
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    @Test
    fun `returns Failed when errorMessage is present even if status is Succeeded`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            errorMessage = "OCR extraction failed"
        )
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    @Test
    fun `returns Ready when errorMessage is whitespace-only`() {
        // Whitespace-only is effectively blank, so not treated as error
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            errorMessage = "   ",
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    // === QUEUED CASES ===

    @Test
    fun `returns Queued when ingestion status is Queued`() {
        val record = createRecord(ingestionStatus = IngestionStatus.Queued)
        assertEquals(DocumentUiStatus.Queued, record.toUiStatus())
    }

    @Test
    fun `returns Queued when latestIngestion is null`() {
        val record = createRecord(latestIngestion = null)
        assertEquals(DocumentUiStatus.Queued, record.toUiStatus())
    }

    // === PROCESSING CASES ===

    @Test
    fun `returns Processing when ingestion status is Processing`() {
        val record = createRecord(ingestionStatus = IngestionStatus.Processing)
        assertEquals(DocumentUiStatus.Processing, record.toUiStatus())
    }

    // === CONFIRMED CASES ===

    @Test
    fun `returns Ready when draft is Confirmed with invoice type`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    @Test
    fun `returns Ready when draft is Confirmed`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    // === REVIEW CASES ===

    @Test
    fun `returns Review when draft needs review`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when draft is rejected`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            documentStatus = DocumentStatus.Rejected,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when succeeded but draft is null`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            draft = null
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    // === EDGE CASES ===

    @Test
    fun `Failed takes priority over Ready conditions`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Failed, // Failed status
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    @Test
    fun `error message takes priority over Succeeded status`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            errorMessage = "Partial extraction failure",
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice
        )
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    // === TEST HELPERS ===

    private fun createRecord(
        ingestionStatus: IngestionStatus = IngestionStatus.Queued,
        errorMessage: String? = null,
        confidence: Double? = null,
        linkedContactId: ContactId? = null,
        documentStatus: DocumentStatus = DocumentStatus.NeedsReview,
        documentType: DocumentType? = null,
        draft: DocumentDraftDto? = createDraft(
            linkedContactId = linkedContactId,
            documentStatus = documentStatus,
            documentType = documentType
        ),
        latestIngestion: DocumentIngestionDto? = createIngestion(ingestionStatus, errorMessage, confidence)
    ): DocumentRecordDto {
        return DocumentRecordDto(
            document = createDocument(),
            draft = draft,
            latestIngestion = latestIngestion,
            confirmedEntity = null
        )
    }

    private fun createIngestion(
        status: IngestionStatus,
        errorMessage: String?,
        confidence: Double?
    ): DocumentIngestionDto {
        val now = LocalDateTime(2024, 1, 1, 12, 0, 0)
        return DocumentIngestionDto(
            id = IngestionRunId.generate(),
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            status = status,
            provider = "openai",
            queuedAt = now,
            startedAt = if (status != IngestionStatus.Queued) now else null,
            finishedAt = if (status == IngestionStatus.Succeeded || status == IngestionStatus.Failed) now else null,
            errorMessage = errorMessage,
            confidence = confidence
        )
    }

    private fun createDraft(
        linkedContactId: ContactId?,
        documentStatus: DocumentStatus,
        documentType: DocumentType?
    ): DocumentDraftDto {
        val now = LocalDateTime(2024, 1, 1, 12, 0, 0)
        return DocumentDraftDto(
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            documentStatus = documentStatus,
            documentType = documentType,
            extractedData = null,
            aiDraftData = null,
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = linkedContactId,
            counterpartyIntent = CounterpartyIntent.None,
            rejectReason = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun createDocument(): DocumentDto {
        return DocumentDto(
            id = DocumentId.generate(),
            tenantId = TenantId.generate(),
            filename = "test-invoice.pdf",
            contentType = "application/pdf",
            sizeBytes = 12345,
            storageKey = "documents/test-invoice.pdf",
            uploadedAt = LocalDateTime(2024, 1, 1, 12, 0, 0)
        )
    }
}
