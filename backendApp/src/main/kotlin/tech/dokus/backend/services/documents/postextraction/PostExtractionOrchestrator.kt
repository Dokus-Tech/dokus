package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.backend.services.documents.IntakeResolution
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.models.toAuthoritativeCounterpartySnapshot
import tech.dokus.foundation.backend.utils.loggerFor

internal class PostExtractionOrchestrator(
    private val documentRepository: DocumentRepository,
    private val documentTruthService: DocumentTruthService,
    private val resolveContact: ResolveDocumentContactUseCase,
    private val processBankStatement: ProcessBankStatementUseCase,
    private val enrichPurpose: EnrichDocumentPurposeUseCase,
    private val autoConfirm: AutoConfirmDocumentUseCase,
) {
    private val logger = loggerFor()

    suspend fun orchestrate(context: PostExtractionContext): PostExtractionOutcome {
        val draftData = context.draftData
            ?: return PostExtractionOutcome.Skipped

        // Unsupported types: auto-confirm immediately, skip enrichment
        if (!context.documentType.supported) {
            documentRepository.updateDocumentStatus(
                documentId = context.documentId,
                tenantId = context.tenantId,
                status = DocumentStatus.Unsupported,
            )
            return PostExtractionOutcome.UnsupportedConfirmed
        }

        // Post-extraction matching (dedup, source merging)
        val matchOutcome = documentTruthService.applyPostExtractionMatching(
            tenantId = context.tenantId,
            documentId = context.documentId,
            sourceId = context.sourceId,
            draftData = draftData,
            extractedSnapshotJson = json.encodeToString(draftData),
        )
        if (matchOutcome.documentId != context.documentId ||
            matchOutcome.resolution is IntakeResolution.NeedsReview
        ) {
            logger.info(
                "Document {} source {} resolved by truth matcher: resolution={}, target={}",
                context.documentId,
                context.sourceId,
                matchOutcome.resolution,
                matchOutcome.documentId,
            )
            return PostExtractionOutcome.TruthResolved(matchOutcome)
        }

        // Bank statement path
        if (draftData is BankStatementDraftData) {
            return processBankStatement(context)
                ?: PostExtractionOutcome.BankStatementProcessed // duplicate → still "processed"
        }

        // Standard document path: set NeedsReview, resolve contact, enrich, auto-confirm
        documentRepository.updateDocumentStatus(
            documentId = context.documentId,
            tenantId = context.tenantId,
            status = DocumentStatus.NeedsReview,
        )

        var linkedContactId: tech.dokus.domain.ids.ContactId? = null
        val authoritativeSnapshot = context.extraction.toAuthoritativeCounterpartySnapshot()
        if (authoritativeSnapshot == null) {
            logger.warn(
                "Missing authoritative counterparty snapshot for document {}; forcing PendingReview",
                context.documentId,
            )
            documentRepository.updateContactResolution(
                documentId = context.documentId,
                tenantId = context.tenantId,
                counterpartySnapshot = null,
                counterparty = CounterpartyInfo.Unresolved(),
            )
        } else {
            linkedContactId = resolveContact(
                tenantId = context.tenantId,
                documentId = context.documentId,
                draftData = draftData,
                authoritativeSnapshot = authoritativeSnapshot,
                tenantVat = context.tenantVat,
            )
        }

        enrichPurpose(
            tenantId = context.tenantId,
            documentId = context.documentId,
            documentType = context.documentType,
            draftData = draftData,
            linkedContactId = linkedContactId,
        )

        autoConfirm(context, linkedContactId)

        return PostExtractionOutcome.StandardProcessed
    }
}
