package tech.dokus.backend.services.documents.confirmation

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.enums.DocumentDirection
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
        is InvoiceDraftData -> when (draftData.direction) {
            DocumentDirection.Inbound -> billService.confirm(
                tenantId = tenantId,
                documentId = documentId,
                draftData = draftData.toBillDraft(),
                linkedContactId = linkedContactId
            )
            DocumentDirection.Outbound,
            DocumentDirection.Unknown -> invoiceService.confirm(tenantId, documentId, draftData, linkedContactId)
        }
        is BillDraftData -> billService.confirm(tenantId, documentId, draftData, linkedContactId)
        is ReceiptDraftData -> receiptService.confirm(tenantId, documentId, draftData, linkedContactId)
        is CreditNoteDraftData -> creditNoteService.confirm(tenantId, documentId, draftData, linkedContactId)
    }

    private fun InvoiceDraftData.toBillDraft(): BillDraftData {
        return BillDraftData(
            direction = DocumentDirection.Inbound,
            supplierName = seller.name ?: customerName,
            supplierVat = seller.vat ?: customerVat,
            invoiceNumber = invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            currency = currency,
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            lineItems = lineItems,
            vatBreakdown = vatBreakdown,
            iban = iban ?: seller.iban,
            payment = payment,
            notes = notes,
            seller = seller,
            buyer = buyer
        )
    }
}
