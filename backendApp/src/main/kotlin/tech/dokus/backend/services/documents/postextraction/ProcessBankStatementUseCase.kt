package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.banking.BankStatementProcessingService
import tech.dokus.backend.services.banking.StatementDedupService.StatementDedupOutcome
import tech.dokus.backend.services.cashflow.matching.MatchingEngine
import tech.dokus.database.repository.cashflow.DocumentRepository
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

        val bankProcessing = bankStatementProcessingService.process(
            tenantId = context.tenantId,
            documentId = context.documentId,
            sourceId = context.sourceId,
            draftData = draftData,
            source = context.bankTransactionSource ?: BankTransactionSource.PdfStatement,
        )
        if (bankProcessing.dedupOutcome is StatementDedupOutcome.Skip) {
            logger.info("Bank statement {} skipped (duplicate)", context.documentId)
            return null
        }

        documentRepository.updateExtractedDataAndStatus(
            documentId = context.documentId,
            tenantId = context.tenantId,
            extractedData = bankProcessing.sanitizedDraft,
            status = DocumentStatus.NeedsReview,
        )

        if (bankProcessing.validRows > 0) {
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
                    draftData = bankProcessing.sanitizedDraft,
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

        // Auto-confirm (same logic as standard documents)
        autoConfirm(context, linkedContactId)

        logger.info(
            "Processed bank statement {}: validRows={}, discardedRows={}, trust={}, dedup={}",
            context.documentId,
            bankProcessing.validRows,
            bankProcessing.discardedRows.size,
            bankProcessing.statementTrust,
            bankProcessing.dedupOutcome,
        )
        return PostExtractionOutcome.BankStatementProcessed
    }
}
