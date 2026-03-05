package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.services.documents.DocumentPurposeSimilarityService
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Dispatches document confirmation to the appropriate type-specific service.
 * Eliminates duplicate `when(draftData)` blocks in routes and workers.
 */
class DocumentConfirmationDispatcher(
    private val invoiceService: InvoiceConfirmationService,
    private val receiptService: ReceiptConfirmationService,
    private val creditNoteService: CreditNoteConfirmationService,
    private val purposeSimilarityService: DocumentPurposeSimilarityService,
) {
    private val logger = loggerFor()

    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: DocumentDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> {
        val confirmation = when (draftData) {
            is InvoiceDraftData -> invoiceService.confirm(tenantId, documentId, draftData, linkedContactId)
            is ReceiptDraftData -> receiptService.confirm(tenantId, documentId, draftData, linkedContactId)
            is CreditNoteDraftData -> creditNoteService.confirm(tenantId, documentId, draftData, linkedContactId)
        }

        confirmation.onSuccess {
            runSuspendCatching {
                purposeSimilarityService.indexConfirmedDocument(
                    tenantId = tenantId,
                    documentId = documentId
                )
            }.onFailure { error ->
                logger.warn(
                    "Purpose similarity indexing failed after confirmation for document {}: {}",
                    documentId,
                    error.message
                )
            }
        }

        return confirmation
    }
}
