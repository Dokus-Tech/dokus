package tech.dokus.features.cashflow.presentation.documents.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DocumentRowAttentionTest {

    @Test
    fun `rejected documents are never attention`() {
        val document = createRecord(
            draftStatus = DocumentStatus.Rejected,
            ingestionStatus = IngestionStatus.Failed
        )

        assertFalse(computeNeedsAttention(document))
    }

    @Test
    fun `confirmed without entity is attention`() {
        val document = createRecord(
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded
        )

        assertTrue(computeNeedsAttention(document))
    }

    @Test
    fun `confirmed with entity is not attention`() {
        val documentId = DocumentId.generate()
        val document = createRecord(
            documentId = documentId,
            draftStatus = DocumentStatus.Confirmed,
            ingestionStatus = IngestionStatus.Succeeded,
            confirmedEntity = createConfirmedExpense(documentId)
        )

        assertFalse(computeNeedsAttention(document))
    }

    @Test
    fun `succeeded without draft is attention`() {
        val document = createRecord(
            draftStatus = null,
            ingestionStatus = IngestionStatus.Succeeded
        )

        assertTrue(computeNeedsAttention(document))
    }

    @Test
    fun `queued ingestion is attention`() {
        val document = createRecord(
            draftStatus = null,
            ingestionStatus = IngestionStatus.Queued
        )

        assertTrue(computeNeedsAttention(document))
    }

    @Test
    fun `needs review draft is attention`() {
        val document = createRecord(
            draftStatus = DocumentStatus.NeedsReview,
            ingestionStatus = IngestionStatus.Succeeded
        )

        assertTrue(computeNeedsAttention(document))
    }

    @Test
    fun `no draft and no ingestion is not attention`() {
        val document = createRecord(
            draftStatus = null,
            ingestionStatus = null
        )

        assertFalse(computeNeedsAttention(document))
    }

    private fun createRecord(
        documentId: DocumentId = DocumentId.generate(),
        draftStatus: DocumentStatus?,
        ingestionStatus: IngestionStatus?,
        confirmedEntity: FinancialDocumentDto? = null
    ): DocumentRecordDto {
        return DocumentRecordDto(
            document = DocumentDto(
                id = documentId,
                tenantId = TENANT_ID,
                filename = "test.pdf",
                contentType = "application/pdf",
                sizeBytes = 123L,
                storageKey = "documents/test.pdf",
                uploadedAt = NOW
            ),
            draft = draftStatus?.let { createDraft(documentId, it) },
            latestIngestion = ingestionStatus?.let { createIngestion(documentId, it) },
            confirmedEntity = confirmedEntity
        )
    }

    private fun createDraft(documentId: DocumentId, status: DocumentStatus): DocumentDraftDto {
        return DocumentDraftDto(
            documentId = documentId,
            tenantId = TENANT_ID,
            documentStatus = status,
            documentType = DocumentType.Invoice,
            extractedData = null,
            aiDraftData = null,
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = null,
            lastSuccessfulRunId = null,
            createdAt = NOW,
            updatedAt = NOW
        )
    }

    private fun createIngestion(documentId: DocumentId, status: IngestionStatus): DocumentIngestionDto {
        return DocumentIngestionDto(
            id = IngestionRunId.generate(),
            documentId = documentId,
            tenantId = TENANT_ID,
            status = status,
            provider = "openai",
            queuedAt = NOW,
            startedAt = NOW,
            finishedAt = NOW,
            errorMessage = null,
            confidence = null
        )
    }

    private fun createConfirmedExpense(documentId: DocumentId): FinancialDocumentDto {
        return FinancialDocumentDto.ExpenseDto(
            id = ExpenseId.generate(),
            tenantId = TENANT_ID,
            date = LocalDate(2024, 1, 1),
            merchant = "Vendor",
            amount = Money.fromInt(100),
            category = ExpenseCategory.Other,
            documentId = documentId,
            createdAt = NOW,
            updatedAt = NOW
        )
    }

    private companion object {
        val TENANT_ID: TenantId = TenantId.generate()
        val NOW: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0, 0)
    }
}
