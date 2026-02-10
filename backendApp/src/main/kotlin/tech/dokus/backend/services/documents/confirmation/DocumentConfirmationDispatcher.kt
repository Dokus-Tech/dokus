package tech.dokus.backend.services.documents.confirmation

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

/**
 * Dispatches document confirmation to the appropriate type-specific service.
 * Eliminates duplicate `when(draftData)` blocks in routes and workers.
 */
class DocumentConfirmationDispatcher(
    private val invoiceService: InvoiceConfirmationService,
    private val billService: BillConfirmationService,
    private val receiptService: ReceiptConfirmationService,
    private val creditNoteService: CreditNoteConfirmationService,
) {
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: DocumentDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = when (draftData) {
        is InvoiceDraftData -> invoiceService.confirm(tenantId, documentId, draftData, linkedContactId)
        is BillDraftData -> billService.confirm(tenantId, documentId, draftData, linkedContactId)
        is ReceiptDraftData -> receiptService.confirm(tenantId, documentId, draftData, linkedContactId)
        is CreditNoteDraftData -> creditNoteService.confirm(tenantId, documentId, draftData, linkedContactId)
    }
}
