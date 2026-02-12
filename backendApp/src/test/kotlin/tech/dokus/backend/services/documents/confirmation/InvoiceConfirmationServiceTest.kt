package tech.dokus.backend.services.documents.confirmation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DraftSummary
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.InvoiceDraftData
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InvoiceConfirmationServiceTest {

    private val invoiceRepository = mockk<InvoiceRepository>()
    private val cashflowEntriesService = mockk<CashflowEntriesService>()
    private val draftRepository = mockk<DocumentDraftRepository>()

    private val service = InvoiceConfirmationService(
        invoiceRepository = invoiceRepository,
        cashflowEntriesService = cashflowEntriesService,
        draftRepository = draftRepository
    )

    @Test
    fun `confirm rejects invoice when direction is unknown`() = runBlocking {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val linkedContactId = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb")

        coEvery { draftRepository.getByDocumentId(documentId, tenantId) } returns needsReviewDraft(documentId, tenantId)

        val result = service.confirm(
            tenantId = tenantId,
            documentId = documentId,
            draftData = InvoiceDraftData(direction = DocumentDirection.Unknown),
            linkedContactId = linkedContactId
        )

        assertTrue(result.isFailure)
        assertIs<DokusException.BadRequest>(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull()?.message?.contains("direction is unknown") == true)

        coVerify(exactly = 0) { invoiceRepository.createInvoice(any(), any()) }
        coVerify(exactly = 0) {
            cashflowEntriesService.createFromInvoice(any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    private fun needsReviewDraft(documentId: DocumentId, tenantId: TenantId): DraftSummary {
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)
        return DraftSummary(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice,
            extractedData = null,
            aiDraftData = null,
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
