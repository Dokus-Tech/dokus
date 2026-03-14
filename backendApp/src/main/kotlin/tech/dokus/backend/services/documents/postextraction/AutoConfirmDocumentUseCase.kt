package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.documents.AutoConfirmInput
import tech.dokus.backend.services.documents.AutoConfirmPolicy
import tech.dokus.backend.services.documents.confirmation.DocumentConfirmationDispatcher
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.ai.models.DirectionResolutionSource
import tech.dokus.foundation.backend.utils.loggerFor

internal class AutoConfirmDocumentUseCase(
    private val autoConfirmPolicy: AutoConfirmPolicy,
    private val confirmationDispatcher: DocumentConfirmationDispatcher,
    private val documentRepository: DocumentRepository,
) {
    private val logger = loggerFor()

    suspend operator fun invoke(
        context: PostExtractionContext,
        linkedContactId: ContactId?,
    ): Boolean {
        val draftData = context.draftData ?: return false

        val input = AutoConfirmInput(
            source = context.sourceChannel,
            documentType = context.documentType,
            draftData = draftData,
            auditPassed = context.auditPassed,
            confidence = context.confidence,
            contactId = linkedContactId,
            directionResolvedFromAiHintOnly = context.directionSource == DirectionResolutionSource.AiHint,
        )

        val rejection = autoConfirmPolicy.evaluate(input)
        if (rejection != null) {
            logger.debug("Auto-confirm rejected for {}: {}", context.documentId, rejection)
            return false
        }

        return try {
            confirmationDispatcher.confirm(
                context.tenantId,
                context.documentId,
                draftData,
                linkedContactId,
            ).getOrThrow()
            true
        } catch (e: Exception) {
            logger.error("Auto-confirm failed for document ${context.documentId}", e)
            documentRepository.updateDocumentStatus(
                documentId = context.documentId,
                tenantId = context.tenantId,
                status = DocumentStatus.NeedsReview,
            )
            false
        }
    }
}
