package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.documents.DocumentPurposeService
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

internal class EnrichDocumentPurposeUseCase(
    private val purposeService: DocumentPurposeService,
    private val documentRepository: DocumentRepository,
) {
    private val logger = loggerFor()

    suspend operator fun invoke(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        draftData: DocumentDraftData,
        linkedContactId: ContactId?,
    ) {
        val currentDraft = documentRepository.getDraftByDocumentId(documentId, tenantId) ?: return
        runSuspendCatching {
            purposeService.enrichAfterContactResolution(
                tenantId = tenantId,
                documentId = documentId,
                documentType = documentType,
                draftData = draftData,
                linkedContactId = linkedContactId,
                currentDraft = currentDraft,
            )
        }.onFailure { e ->
            logger.warn("Purpose enrichment failed for document {}, skipping", documentId, e)
        }
    }
}
