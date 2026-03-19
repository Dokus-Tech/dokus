package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.banking.BankStatementProcessingService
import tech.dokus.backend.services.banking.StatementDedupService.StatementDedupOutcome
import tech.dokus.backend.services.cashflow.matching.MatchingEngine
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.features.ai.models.toAuthoritativeCounterpartySnapshot
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

internal class ProcessBankStatementUseCase(
    private val bankStatementProcessingService: BankStatementProcessingService,
    private val matchingEngine: MatchingEngine,
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
            source = context.bankTransactionSource ?: BankTransactionSource.PdfStatement,
        )
        if (prepareResult.dedupOutcome is StatementDedupOutcome.Skip) {
            logger.info("Bank statement {} skipped (duplicate)", context.documentId)
            return null
        }

        // Store the annotated draft (with duplicate flags and validated rows)
        documentRepository.updateExtractedDataAndStatus(
            documentId = context.documentId,
            tenantId = context.tenantId,
            extractedData = prepareResult.sanitizedDraft,
            status = DocumentStatus.NeedsReview,
        )

        if (prepareResult.hasDuplicates) {
            // Defer persistence — user must review duplicates and confirm
            logger.info(
                "Bank statement {} has potential duplicates, deferring to user review",
                context.documentId,
            )
        } else if (prepareResult.validRows > 0) {
            // No duplicates — persist immediately and run matching (happy path)
            bankStatementProcessingService.persistTransactions(
                tenantId = context.tenantId,
                documentId = context.documentId,
                prepareResult = prepareResult,
            )

            runSuspendCatching {
                matchingEngine.matchBankStatement(
                    tenantId = context.tenantId,
                    documentId = context.documentId,
                )
            }.onFailure {
                logger.warn(
                    "Bank statement matching failed for {}: {}",
                    context.documentId,
                    it.message,
                )
            }
        }

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

        // Auto-confirm only when no duplicates detected
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
