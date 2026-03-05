package tech.dokus.backend.services.documents.confirmation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.documents.DocumentPurposeSimilarityService
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import kotlin.test.assertTrue

class DocumentConfirmationDispatcherTest {

    private val invoiceService = mockk<InvoiceConfirmationService>()
    private val receiptService = mockk<ReceiptConfirmationService>()
    private val creditNoteService = mockk<CreditNoteConfirmationService>()
    private val purposeSimilarityService = mockk<DocumentPurposeSimilarityService>(relaxed = true)

    private val dispatcher = DocumentConfirmationDispatcher(
        invoiceService = invoiceService,
        receiptService = receiptService,
        creditNoteService = creditNoteService,
        purposeSimilarityService = purposeSimilarityService
    )

    private val tenantId = TenantId.parse("11111111-1111-1111-1111-111111111111")
    private val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")

    @Test
    fun `confirm success triggers purpose index`() = runBlocking {
        val result = ConfirmationResult(
            entity = mockk<FinancialDocumentDto>(),
            cashflowEntryId = null,
            documentId = documentId
        )
        coEvery { invoiceService.confirm(tenantId, documentId, any(), any()) } returns Result.success(result)

        val confirmation = dispatcher.confirm(
            tenantId = tenantId,
            documentId = documentId,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            linkedContactId = null
        )

        assertTrue(confirmation.isSuccess)
        coVerify(exactly = 1) { purposeSimilarityService.indexConfirmedDocument(tenantId, documentId) }
    }

    @Test
    fun `confirm failure does not trigger purpose index`() = runBlocking {
        coEvery {
            invoiceService.confirm(tenantId, documentId, any(), any())
        } returns Result.failure(IllegalStateException("boom"))

        val confirmation = dispatcher.confirm(
            tenantId = tenantId,
            documentId = documentId,
            draftData = InvoiceDraftData(direction = DocumentDirection.Inbound),
            linkedContactId = null
        )

        assertTrue(confirmation.isFailure)
        coVerify(exactly = 0) { purposeSimilarityService.indexConfirmedDocument(any(), any()) }
    }
}
