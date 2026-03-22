package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.banking.BankStatementProcessingService
import tech.dokus.backend.services.banking.StatementDedupService.StatementDedupOutcome
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.features.ai.models.toAuthoritativeCounterpartySnapshot
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Processes a bank statement after AI extraction.
 *
 * Flow:
 * 1. Validate rows, resolve account, calculate trust, detect duplicates
 * 2. Store annotated draft (transactions with duplicate flags)
 * 3. Resolve bank institution contact
 * 4. Auto-confirm if policy passes (confirmation dispatcher persists transactions)
 *
 * Transaction persistence is always deferred to confirmation —
 * same pattern as invoices/receipts/credit notes.
 */
internal class ProcessBankStatementUseCase(
    private val bankStatementProcessingService: BankStatementProcessingService,
    private val documentRepository: DocumentRepository,
    private val resolveContact: ResolveDocumentContactUseCase,
    private val autoConfirm: AutoConfirmDocumentUseCase,
) {
    private val logger = loggerFor()

    /**
     * Returns `null` if the statement was skipped as a duplicate.
     */
    suspend operator fun invoke(context: PostExtractionContext): PostExtractionOutcome? {
        val draftData = context.draftData as BankStatementDraftData

        val prepareResult = bankStatementProcessingService.prepare(
            tenantId = context.tenantId,
            documentId = context.documentId,
            sourceId = context.sourceId,
            draftData = draftData,
        )

        if (prepareResult.dedupOutcome is StatementDedupOutcome.Skip) {
            logger.info("Bank statement {} skipped (duplicate)", context.documentId)
            return null
        }

        // Store annotated draft with validated transactions and duplicate flags
        documentRepository.updateExtractedDataAndStatus(
            documentId = context.documentId,
            tenantId = context.tenantId,
            extractedData = prepareResult.sanitizedDraft,
            status = DocumentStatus.NeedsReview,
        )

        // Contact resolution for bank institution
        var linkedContactId: tech.dokus.domain.ids.ContactId? = null
        val authoritativeSnapshot = context.extraction.toAuthoritativeCounterpartySnapshot()
        if (authoritativeSnapshot != null) {
            runSuspendCatching {
                resolveContact(
                    tenantId = context.tenantId,
                    documentId = context.documentId,
                    draftData = prepareResult.sanitizedDraft,
                    authoritativeSnapshot = authoritativeSnapshot,
                    tenantVat = context.tenantVat,
                )
            }.onSuccess { contactId ->
                linkedContactId = contactId
            }.onFailure {
                logger.warn(
                    "Contact resolution failed for bank statement {}: {}",
                    context.documentId,
                    it.message,
                )
            }
        }

        // Auto-confirm (policy + confirmation dispatcher handle persistence + matching)
        if (!prepareResult.hasDuplicates) {
            autoConfirm(context, linkedContactId)
        }

        logger.info(
            "Processed bank statement {}: validRows={}, discardedRows={}, trust={}, dedup={}, duplicates={}",
            context.documentId,
            prepareResult.validRows,
            prepareResult.discardedRows.size,
            prepareResult.statementTrust,
            prepareResult.dedupOutcome,
            prepareResult.hasDuplicates,
        )
        return PostExtractionOutcome.BankStatementProcessed
    }
}
