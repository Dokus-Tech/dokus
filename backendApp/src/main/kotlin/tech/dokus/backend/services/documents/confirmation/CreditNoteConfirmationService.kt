package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.services.cashflow.CreditNoteService
import tech.dokus.backend.util.isUniqueViolation
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.documents.DocumentLinkRepository
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.DocumentDirection
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
    private val documentLinkRepository: DocumentLinkRepository,
    private val invoiceRepository: InvoiceRepository
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

        val draft = requireConfirmableDraft(draftRepository, tenantId, documentId)
        val isReconfirm = draft.documentStatus == DocumentStatus.NeedsReview

        val contactId = linkedContactId ?: draft.linkedContactId
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

        val existingCreditNote = creditNoteService.findByDocumentId(tenantId, documentId)
        val requestBase = CreateCreditNoteRequest(
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

        val updatedOrCreated = when {
            existingCreditNote == null -> {
                creditNoteService.createCreditNote(
                    tenantId = tenantId,
                    request = requestBase
                ).getOrElse { t ->
                    if (!t.isUniqueViolation()) throw t
                    creditNoteService.findByDocumentId(tenantId, documentId) ?: throw t
                }
            }

            isReconfirm -> {
                if (existingCreditNote.status !in setOf(CreditNoteStatus.Draft, CreditNoteStatus.Confirmed)) {
                    throw DokusException.BadRequest(
                        "Cannot re-confirm credit note in status: ${existingCreditNote.status}"
                    )
                }

                val request = requestBase.copy(settlementIntent = existingCreditNote.settlementIntent)
                creditNoteService.updateCreditNote(existingCreditNote.id, tenantId, request).getOrThrow()
            }

            else -> existingCreditNote
        }

        val confirmed = if (updatedOrCreated.status == CreditNoteStatus.Draft) {
            creditNoteService.confirmCreditNote(updatedOrCreated.id, tenantId).getOrThrow()
        } else {
            updatedOrCreated
        }

        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)
        upsertOriginalReferenceLink(
            tenantId = tenantId,
            creditNoteDocumentId = documentId,
            draftData = draftData
        )

        logger.info("Credit note confirmed: $documentId -> creditNoteId=${confirmed.id}")
        ConfirmationResult(entity = confirmed, cashflowEntryId = null, documentId = documentId)
    }

    private suspend fun upsertOriginalReferenceLink(
        tenantId: TenantId,
        creditNoteDocumentId: DocumentId,
        draftData: CreditNoteDraftData
    ) {
        val originalInvoiceNumber = draftData.originalInvoiceNumber
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return

        val targetDocumentId = invoiceRepository
            .findByInvoiceNumber(tenantId, originalInvoiceNumber)
            ?.documentId

        documentLinkRepository.upsertOriginalDocumentLink(
            tenantId = tenantId,
            sourceDocumentId = creditNoteDocumentId,
            targetDocumentId = targetDocumentId,
            externalReference = if (targetDocumentId == null) originalInvoiceNumber else null
        )
    }
}
