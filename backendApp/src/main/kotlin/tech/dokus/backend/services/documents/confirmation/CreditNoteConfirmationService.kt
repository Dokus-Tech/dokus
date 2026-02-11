package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.services.cashflow.CreditNoteService
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateCreditNoteRequest
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Confirms CreditNote documents via the dedicated [CreditNoteService].
 *
 * Credit notes have a different flow from other document types:
 * - No cashflow entry created on confirmation (created later on refund)
 * - Uses CreditNoteService which manages its own transactions
 */
class CreditNoteConfirmationService(
    private val creditNoteService: CreditNoteService,
    private val draftRepository: DocumentDraftRepository,
) {
    private val logger = loggerFor()

    @Suppress("ThrowsCount")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: CreditNoteDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runSuspendCatching {
        logger.info("Confirming credit note document: $documentId for tenant: $tenantId")

        ensureDraftConfirmable(draftRepository, tenantId, documentId)

        val contactId = linkedContactId
            ?: throw DokusException.BadRequest("Credit note requires a linked contact")
        val creditNoteType = when (draftData.direction) {
            DocumentDirection.Outbound -> CreditNoteType.Sales
            DocumentDirection.Inbound -> CreditNoteType.Purchase
            DocumentDirection.Unknown -> throw DokusException.BadRequest("Credit note direction is unknown")
        }
        val creditNoteNumber = draftData.creditNoteNumber
            ?: throw DokusException.BadRequest("Credit note number is required")
        val issueDate = draftData.issueDate
            ?: throw DokusException.BadRequest("Credit note issue date is required")
        val subtotalAmount = draftData.subtotalAmount
            ?: throw DokusException.BadRequest("Credit note subtotal amount is required")
        val vatAmount = draftData.vatAmount
            ?: throw DokusException.BadRequest("Credit note VAT amount is required")
        val totalAmount = draftData.totalAmount
            ?: throw DokusException.BadRequest("Credit note total amount is required")

        val created = creditNoteService.createCreditNote(
            tenantId = tenantId,
            request = CreateCreditNoteRequest(
                contactId = contactId,
                creditNoteType = creditNoteType,
                creditNoteNumber = creditNoteNumber,
                issueDate = issueDate,
                subtotalAmount = subtotalAmount,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                currency = draftData.currency,
                settlementIntent = SettlementIntent.Unknown,
                reason = draftData.reason,
                notes = draftData.notes,
                documentId = documentId
            )
        ).getOrThrow()

        val confirmed = creditNoteService.confirmCreditNote(created.id, tenantId).getOrThrow()

        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        logger.info("Credit note confirmed: $documentId -> creditNoteId=${created.id}")
        ConfirmationResult(entity = confirmed, cashflowEntryId = null, documentId = documentId)
    }
}
