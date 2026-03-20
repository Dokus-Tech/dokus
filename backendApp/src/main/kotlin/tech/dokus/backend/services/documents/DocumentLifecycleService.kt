package tech.dokus.backend.services.documents

import tech.dokus.backend.routes.cashflow.documents.ConfirmedBankStatement
import tech.dokus.backend.routes.cashflow.documents.confirmedEntityToDocDto
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.backend.routes.cashflow.documents.toSummaryDto
import tech.dokus.backend.routes.cashflow.documents.updateDraftCounterparty
import tech.dokus.backend.services.cashflow.CashflowProjectionReconciliationService
import tech.dokus.backend.services.cashflow.InvoiceBankAutomationService
import tech.dokus.backend.services.contacts.ContactService
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.backend.services.documents.sse.DocumentSsePublisher
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DocumentSourceRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.isLinked
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.model.toDraftData
import tech.dokus.domain.model.toDocumentType
import tech.dokus.foundation.backend.storage.DocumentStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Service for document lifecycle operations: confirm, unconfirm, reject, delete,
 * draft CRUD, reprocessing, content download, and ingestion history.
 */
internal class DocumentLifecycleService(
    private val documentRepository: DocumentRepository,
    private val draftRepository: DraftRepository,
    private val invoiceRepository: InvoiceRepository,
    private val expenseRepository: ExpenseRepository,
    private val creditNoteRepository: CreditNoteRepository,
    private val bankStatementRepository: BankStatementRepository,
    private val bankTransactionRepository: BankTransactionRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val ingestionRepository: DocumentIngestionRunRepository,
    private val documentSourceRepository: DocumentSourceRepository,
    private val confirmationDispatcher: DocumentConfirmationDispatcher,
    private val contactService: ContactService,
    private val projectionReconciliationService: CashflowProjectionReconciliationService,
    private val invoiceBankAutomationService: InvoiceBankAutomationService,
    private val documentRecordLoader: DocumentRecordLoader,
    private val documentSsePublisher: DocumentSsePublisher,
    private val truthService: DocumentTruthService,
    private val purposeService: DocumentPurposeService,
    private val storageService: DocumentStorageService,
) {
    private val logger = loggerFor()

    // ── Find confirmed entity ────────────────────────────────────────

    suspend fun findConfirmedEntity(
        documentId: DocumentId,
        documentType: DocumentType?,
        tenantId: TenantId,
    ): Any? {
        return when (documentType) {
            DocumentType.Invoice -> invoiceRepository.findByDocumentId(tenantId, documentId)
            DocumentType.CreditNote -> creditNoteRepository.findByDocumentId(tenantId, documentId)
            DocumentType.BankStatement -> findConfirmedBankStatement(tenantId, documentId)
            DocumentType.Receipt,
            DocumentType.ProForma,
            DocumentType.Quote,
            DocumentType.OrderConfirmation,
            DocumentType.DeliveryNote,
            DocumentType.Reminder,
            DocumentType.StatementOfAccount,
            DocumentType.PurchaseOrder,
            DocumentType.ExpenseClaim,
            DocumentType.BankFee,
            DocumentType.InterestStatement,
            DocumentType.PaymentConfirmation,
            DocumentType.VatReturn,
            DocumentType.VatListing,
            DocumentType.VatAssessment,
            DocumentType.IcListing,
            DocumentType.OssReturn,
            DocumentType.CorporateTax,
            DocumentType.CorporateTaxAdvance,
            DocumentType.TaxAssessment,
            DocumentType.PersonalTax,
            DocumentType.WithholdingTax,
            DocumentType.SocialContribution,
            DocumentType.SocialFund,
            DocumentType.SelfEmployedContribution,
            DocumentType.Vapz,
            DocumentType.SalarySlip,
            DocumentType.PayrollSummary,
            DocumentType.EmploymentContract,
            DocumentType.Dimona,
            DocumentType.C4,
            DocumentType.HolidayPay,
            DocumentType.Contract,
            DocumentType.Lease,
            DocumentType.Loan,
            DocumentType.Insurance,
            DocumentType.Dividend,
            DocumentType.ShareholderRegister,
            DocumentType.CompanyExtract,
            DocumentType.AnnualAccounts,
            DocumentType.BoardMinutes,
            DocumentType.Subsidy,
            DocumentType.Fine,
            DocumentType.Permit,
            DocumentType.CustomsDeclaration,
            DocumentType.Intrastat,
            DocumentType.DepreciationSchedule,
            DocumentType.Inventory,
            DocumentType.Other,
            DocumentType.Unknown,
            null -> {
                // Try all types
                invoiceRepository.findByDocumentId(tenantId, documentId)
                    ?: expenseRepository.findByDocumentId(tenantId, documentId)
                    ?: creditNoteRepository.findByDocumentId(tenantId, documentId)
                    ?: findConfirmedBankStatement(tenantId, documentId)
            }
        }
    }

    private suspend fun findConfirmedBankStatement(
        tenantId: TenantId,
        documentId: DocumentId,
    ): ConfirmedBankStatement? {
        val statement = bankStatementRepository.findByDocumentId(tenantId, documentId) ?: return null
        return ConfirmedBankStatement(
            statement = statement,
            transactions = bankTransactionRepository.listByDocument(tenantId, documentId),
        )
    }

    // ── Confirm ──────────────────────────────────────────────────────

    data class ConfirmResult(
        val record: DocumentDetailDto,
        val wasAlreadyConfirmed: Boolean,
    )

    suspend fun confirmDocument(
        tenantId: TenantId,
        documentId: DocumentId,
    ): ConfirmResult {
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
            ?: throw DokusException.NotFound("Draft not found for document")

        if (draft.documentStatus == DocumentStatus.Rejected) {
            throw DokusException.BadRequest("Cannot confirm a rejected document")
        }

        val draftType = draft.documentType ?: DocumentType.Unknown

        // Check if already confirmed (idempotent)
        if (draft.documentStatus == DocumentStatus.Confirmed) {
            val confirmedEntity = findConfirmedEntity(documentId, draftType, tenantId)

            if (confirmedEntity != null) {
                if (confirmedEntity is InvoiceEntity &&
                    confirmedEntity.paidAmount.minor >= confirmedEntity.totalAmount.minor &&
                    confirmedEntity.paidAt == null
                ) {
                    logger.warn(
                        "Detected paid invoice without paidAt during confirm-path reconciliation: tenant={}, document={}, invoice={}",
                        tenantId, documentId, confirmedEntity.id
                    )
                }

                projectionReconciliationService
                    .ensureProjectionIfMissing(tenantId, documentId, confirmedEntity)
                    .getOrElse {
                        throw DokusException.InternalError(
                            "Failed to repair missing cashflow projection: ${it.message}"
                        )
                    }

                val existingRecord = documentRecordLoader.load(tenantId, documentId)
                    ?: throw DokusException.NotFound("Document not found")
                return ConfirmResult(record = existingRecord, wasAlreadyConfirmed = true)
            }

            logger.warn(
                "Draft is confirmed but entity not found; proceeding with confirmation: document=$documentId"
            )
        }

        // Check draft is ready
        if (draft.documentStatus != DocumentStatus.NeedsReview &&
            draft.documentStatus != DocumentStatus.Confirmed
        ) {
            throw DokusException.BadRequest("Draft is not ready for confirmation: ${draft.documentStatus}")
        }

        val counterparty = draft.counterparty
        if (counterparty.isUnresolved() && counterparty.pendingCreation) {
            throw DokusException.BadRequest("Counterparty is pending creation")
        }

        if (draftType == DocumentType.Unknown) {
            throw DokusException.BadRequest("Document type must be resolved before confirmation")
        }

        val docDto = draftRepository.getDraftAsDocDto(tenantId, documentId, draftType)
        val draftData = docDto?.toDraftData()
            ?: throw DokusException.BadRequest("No draft data available for confirmation")

        // Check if entity already exists (re-confirm path)
        val existingEntityBeforeConfirm = findConfirmedEntity(documentId, draftType, tenantId)

        // Resolve contact: linked > auto-create from snapshot > null
        val linkedContactId = when {
            counterparty.isLinked() -> counterparty.contactId
            counterparty.isUnresolved() && counterparty.snapshot?.name != null -> {
                val snapshot = counterparty.snapshot!!
                val created = contactService.createContact(
                    tenantId,
                    CreateContactRequest(
                        name = Name(snapshot.name!!),
                        vatNumber = snapshot.vatNumber,
                        iban = snapshot.iban,
                        addresses = snapshot.address?.let { addr ->
                            listOf(
                                ContactAddressInput(
                                    streetLine1 = addr.streetLine1,
                                    streetLine2 = addr.streetLine2,
                                    city = addr.city,
                                    postalCode = addr.postalCode,
                                    country = addr.country?.dbValue,
                                )
                            )
                        } ?: emptyList(),
                        source = ContactSource.AI,
                    )
                ).getOrThrow()
                documentRepository.updateCounterparty(
                    documentId = documentId,
                    tenantId = tenantId,
                    counterparty = CounterpartyInfo.Linked(
                        contactId = created.id,
                        source = ContactLinkSource.AI,
                    ),
                )
                logger.info("Auto-created contact {} for document {} during confirm", created.id, documentId)
                created.id
            }
            else -> null
        }

        // Confirm: creates entity + cashflow entry + marks draft confirmed
        val confirmationResult = confirmationDispatcher.confirm(
            tenantId, documentId, draftData, linkedContactId
        ).getOrThrow()

        val entryId = confirmationResult.cashflowEntryId
        logger.info("Document confirmed: $documentId -> $draftType, cashflowEntryId=$entryId")

        // Clean up draft table row
        val docType = draft.documentType
        if (docType != null) {
            draftRepository.deleteDraft(tenantId, documentId, docType)
        }

        if (confirmationResult.entity is InvoiceEntity && entryId != null) {
            runCatching {
                invoiceBankAutomationService.onInvoiceConfirmed(tenantId = tenantId, entryId = entryId)
            }.onFailure {
                logger.warn(
                    "Invoice-bank automation failed after confirmation for tenant={}, document={}: {}",
                    tenantId, documentId, it.message
                )
            }
        }

        documentSsePublisher.publishDocumentChanged(tenantId, documentId)
        val confirmedRecord = documentRecordLoader.load(tenantId, documentId)
            ?: throw DokusException.NotFound("Document not found")

        return ConfirmResult(
            record = confirmedRecord,
            wasAlreadyConfirmed = existingEntityBeforeConfirm != null,
        )
    }

    // ── Unconfirm ────────────────────────────────────────────────────

    suspend fun unconfirmDocument(tenantId: TenantId, documentId: DocumentId): DocumentDetailDto {
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
            ?: throw DokusException.NotFound("Draft not found for document")

        if (draft.documentStatus != DocumentStatus.Confirmed) {
            throw DokusException.BadRequest("Document is not confirmed")
        }

        val docType = draft.documentType ?: throw DokusException.BadRequest("Document type unknown")

        val confirmedEntity = findConfirmedEntity(documentId, docType, tenantId)
            ?: throw DokusException.NotFound("Confirmed entity not found")

        // Resolve source type and entity ID for cashflow entry lookup
        val cashflowSource = when (confirmedEntity) {
            is InvoiceEntity -> CashflowSourceType.Invoice to confirmedEntity.id.toString()
            is tech.dokus.database.entity.ExpenseEntity -> CashflowSourceType.Expense to confirmedEntity.id.toString()
            else -> null
        }

        // Guard: check cashflow entry has no payments
        if (cashflowSource != null) {
            val (sourceType, sourceIdStr) = cashflowSource
            val entry = cashflowEntriesRepository.getBySource(
                tenantId, sourceType, java.util.UUID.fromString(sourceIdStr)
            ).getOrNull()
            if (entry != null && entry.paidAt != null) {
                throw DokusException.BadRequest("Cannot unconfirm: document has recorded payments")
            }
        }

        // Convert confirmed entity -> DocDto -> DraftData
        val docDto = confirmedEntityToDocDto(confirmedEntity)
        val draftData = docDto.toDraftData()

        // Save to draft table
        draftRepository.saveDraftFromExtraction(tenantId, documentId, draftData)

        // Delete cashflow entry (if exists)
        if (cashflowSource != null) {
            val (sourceType, sourceIdStr) = cashflowSource
            cashflowEntriesRepository.deleteBySource(
                tenantId, sourceType, java.util.UUID.fromString(sourceIdStr)
            )
        }

        // Delete confirmed entity
        when (confirmedEntity) {
            is InvoiceEntity -> invoiceRepository.deleteInvoice(confirmedEntity.id, tenantId)
            is tech.dokus.database.entity.ExpenseEntity -> expenseRepository.deleteExpense(confirmedEntity.id, tenantId)
            is tech.dokus.database.entity.CreditNoteEntity -> creditNoteRepository.deleteCreditNote(confirmedEntity.id, tenantId)
            is ConfirmedBankStatement -> {
                bankTransactionRepository.deleteByDocument(tenantId, documentId)
                bankStatementRepository.deleteByDocumentId(tenantId, documentId)
            }
        }

        documentRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.NeedsReview)
        documentSsePublisher.publishDocumentChanged(tenantId, documentId)

        return documentRecordLoader.load(tenantId, documentId)
            ?: throw DokusException.NotFound("Document not found after unconfirm")
    }

    // ── Reject ───────────────────────────────────────────────────────

    suspend fun rejectDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        request: RejectDocumentRequest,
    ): DocumentDetailDto {
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
            ?: throw DokusException.NotFound("Draft not found for document")

        if (draft.documentStatus == DocumentStatus.Confirmed) {
            throw DokusException.BadRequest("Cannot reject a confirmed document")
        }

        if (draft.documentStatus != DocumentStatus.Rejected) {
            documentRepository.rejectDraft(documentId, tenantId, request.reason)
            documentSsePublisher.publishDocumentChanged(tenantId, documentId)
        }

        return documentRecordLoader.load(tenantId, documentId)
            ?: throw DokusException.NotFound("Document not found")
    }

    // ── Delete ───────────────────────────────────────────────────────

    suspend fun deleteDocument(tenantId: TenantId, documentId: DocumentId) {
        if (!documentRepository.exists(tenantId, documentId)) {
            throw DokusException.NotFound("Document not found")
        }

        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
        if (draft?.documentStatus == DocumentStatus.Confirmed) {
            throw DokusException.BadRequest(
                "Confirmed documents cannot be fully deleted. Remove sources instead."
            )
        }

        val sources = truthService.listSources(tenantId, documentId)
        documentRepository.delete(tenantId, documentId)

        for (source in sources) {
            try {
                storageService.deleteDocument(source.storageKey)
            } catch (e: Exception) {
                logger.warn("Failed to delete source blob from storage: ${source.storageKey}", e)
            }
        }

        documentSsePublisher.publishDocumentDeleted(tenantId, documentId)
    }

    // ── Draft ────────────────────────────────────────────────────────

    suspend fun getDraft(tenantId: TenantId, documentId: DocumentId) =
        documentRepository.getDraftByDocumentId(documentId, tenantId)

    suspend fun updateDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        userId: UserId,
        request: UpdateDraftRequest,
    ): UpdateDraftResponse? {
        val draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
            ?: throw DokusException.NotFound("Draft not found for document")

        val requestData = request.extractedData
        val hasExtractedData = requestData != null
        val hasContactUpdate = request.contactId != null || request.pendingCreation != null
        val hasPurposeUpdate = request.purpose != null || request.purposePeriodMode != null

        if (!hasExtractedData && !hasContactUpdate && !hasPurposeUpdate) {
            throw DokusException.BadRequest("No draft changes provided")
        }
        if (request.purposePeriodMode != null && request.purpose == null) {
            throw DokusException.BadRequest("purposePeriodMode requires purpose")
        }
        if (draft.documentStatus == DocumentStatus.Rejected) {
            throw DokusException.BadRequest("Cannot edit rejected draft")
        }

        if (hasExtractedData) {
            val newVersion = documentRepository.updateDraft(
                documentId = documentId,
                tenantId = tenantId,
                userId = userId,
                updatedData = requestData
            ) ?: throw DokusException.InternalError("Failed to update draft")

            if (requestData != null) {
                val oldType = draft.documentType
                val newType = requestData.toDocumentType()
                if (oldType != null && oldType != newType) {
                    draftRepository.deleteDraft(tenantId, documentId, oldType)
                }
                draftRepository.saveDraftFromExtraction(tenantId, documentId, requestData)
            }

            if (hasContactUpdate) {
                updateDraftCounterparty(documentRepository, documentId, tenantId, request)
            }
            if (hasPurposeUpdate) {
                val purpose = request.purpose
                    ?: throw DokusException.BadRequest("purpose is required for purpose update")
                purposeService.applyUserPurposeEdit(
                    tenantId = tenantId,
                    documentId = documentId,
                    draft = documentRepository.getDraftByDocumentId(documentId, tenantId)
                        ?: throw DokusException.NotFound("Draft not found for document"),
                    purpose = purpose,
                    purposePeriodMode = request.purposePeriodMode
                )
            }

            documentSsePublisher.publishDocumentChanged(tenantId, documentId)
            return UpdateDraftResponse(
                documentId = documentId,
                draftVersion = newVersion,
                extractedData = requestData,
                updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        } else {
            if (hasContactUpdate) {
                updateDraftCounterparty(documentRepository, documentId, tenantId, request)
            }
            if (hasPurposeUpdate) {
                val purpose = request.purpose
                    ?: throw DokusException.BadRequest("purpose is required for purpose update")
                purposeService.applyUserPurposeEdit(
                    tenantId = tenantId,
                    documentId = documentId,
                    draft = draft,
                    purpose = purpose,
                    purposePeriodMode = request.purposePeriodMode
                )
            }
            documentSsePublisher.publishDocumentChanged(tenantId, documentId)
            return null // signals NoContent response
        }
    }

    // ── Ingestion ────────────────────────────────────────────────────

    suspend fun getIngestionHistory(tenantId: TenantId, documentId: DocumentId) =
        ingestionRepository.listByDocument(documentId, tenantId)

    suspend fun reprocessDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        request: ReprocessRequest,
    ): ReprocessResponse {
        if (!documentRepository.exists(tenantId, documentId)) {
            throw DokusException.NotFound("Document not found")
        }

        // Idempotent: return existing active run
        if (!request.force) {
            val activeRun = ingestionRepository.findActiveRun(documentId, tenantId)
            if (activeRun != null) {
                return ReprocessResponse(
                    runId = activeRun.id,
                    status = activeRun.status,
                    message = "Existing ${activeRun.status.name.lowercase()} run found",
                    isExistingRun = true
                )
            }
        }

        val runId = ingestionRepository.createRun(
            documentId = documentId,
            tenantId = tenantId,
            userFeedback = request.userFeedback,
            overrideMaxPages = request.maxPages,
            overrideDpi = request.dpi,
        )

        documentSsePublisher.publishDocumentChanged(tenantId, documentId)
        return ReprocessResponse(
            runId = runId,
            status = IngestionStatus.Queued,
            message = "Document queued for processing",
            isExistingRun = false
        )
    }

    // ── Content download helpers ─────────────────────────────────────

    data class ContentDownloadInfo(
        val storageKey: String,
        val filename: String,
        val contentType: String,
    )

    suspend fun resolveDocumentContent(tenantId: TenantId, documentId: DocumentId): ContentDownloadInfo {
        val document = truthService.getDocument(tenantId, documentId)
            ?: throw DokusException.NotFound("Document not found")
        val sources = truthService.listSources(tenantId, documentId)
        val source = tech.dokus.database.repository.cashflow.selectPreferredSource(sources)
            ?: throw DokusException.NotFound("No source available for document")

        return ContentDownloadInfo(
            storageKey = source.storageKey,
            filename = source.filename ?: document.filename,
            contentType = source.contentType,
        )
    }

    suspend fun resolveSourceContent(
        tenantId: TenantId,
        documentId: DocumentId,
        sourceId: DocumentSourceId,
    ): ContentDownloadInfo {
        if (!documentRepository.exists(tenantId, documentId)) {
            throw DokusException.NotFound("Document not found")
        }
        val source = documentSourceRepository.getById(tenantId, sourceId)
            ?: throw DokusException.NotFound("Source not found")
        if (source.documentId != documentId || source.status != DocumentSourceStatus.Linked) {
            throw DokusException.NotFound("Source not found")
        }

        return ContentDownloadInfo(
            storageKey = source.storageKey,
            filename = source.filename ?: "${documentId}_$sourceId",
            contentType = source.contentType,
        )
    }

    // ── Sources listing ──────────────────────────────────────────────

    suspend fun listSources(tenantId: TenantId, documentId: DocumentId) =
        truthService.listSources(tenantId, documentId)
}
