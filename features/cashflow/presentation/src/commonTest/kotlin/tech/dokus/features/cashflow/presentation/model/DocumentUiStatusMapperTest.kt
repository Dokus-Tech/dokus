package tech.dokus.features.cashflow.presentation.model

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DraftStatus
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
    fun `returns Review when errorMessage is whitespace-only`() {
        // Whitespace-only is effectively blank, so not treated as error
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            errorMessage = "   ",
            confidence = 0.50
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
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

    // === READY CASES ===

    @Test
    fun `returns Ready when succeeded with linked contact and high confidence`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.95,
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    @Test
    fun `returns Ready at exact threshold boundary`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.90, // Exactly at threshold
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    @Test
    fun `returns Ready when confidence is 1_0`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 1.0,
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Ready, record.toUiStatus())
    }

    // === REVIEW CASES ===

    @Test
    fun `returns Review when succeeded but no linked contact`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.95,
            linkedContactId = null
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when succeeded with linked contact but low confidence`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.89, // Below threshold
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when succeeded but draft is null`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.95,
            draft = null
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when confidence is null`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = null,
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    @Test
    fun `returns Review when confidence is zero`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            confidence = 0.0,
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Review, record.toUiStatus())
    }

    // === EDGE CASES ===

    @Test
    fun `Failed takes priority over Ready conditions`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Failed, // Failed status
            confidence = 0.99, // Would be Ready otherwise
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    @Test
    fun `error message takes priority over Succeeded status`() {
        val record = createRecord(
            ingestionStatus = IngestionStatus.Succeeded,
            errorMessage = "Partial extraction failure",
            confidence = 0.95,
            linkedContactId = ContactId.generate()
        )
        assertEquals(DocumentUiStatus.Failed, record.toUiStatus())
    }

    // === TEST HELPERS ===

    private fun createRecord(
        ingestionStatus: IngestionStatus = IngestionStatus.Queued,
        errorMessage: String? = null,
        confidence: Double? = null,
        linkedContactId: ContactId? = null,
        draft: DocumentDraftDto? = createDraft(linkedContactId),
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

    private fun createDraft(linkedContactId: ContactId?): DocumentDraftDto {
        val now = LocalDateTime(2024, 1, 1, 12, 0, 0)
        return DocumentDraftDto(
            documentId = DocumentId.generate(),
            tenantId = TenantId.generate(),
            draftStatus = DraftStatus.NeedsReview,
            documentType = null,
            extractedData = null,
            aiDraftData = null,
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            suggestedContactId = null,
            contactSuggestionConfidence = null,
            contactSuggestionReason = null,
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
