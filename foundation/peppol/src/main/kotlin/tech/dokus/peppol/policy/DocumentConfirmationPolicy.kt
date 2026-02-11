package tech.dokus.peppol.policy

import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftData

/**
 * Policy interface for determining if a document can be auto-confirmed.
 *
 * Extensible for future AI confidence thresholds (Dokus One).
 * Currently uses simple source-based rules.
 */
interface DocumentConfirmationPolicy {

    /**
     * Determine if a document can be automatically confirmed without user review.
     *
     * @param source Where the document came from
     * @param draftData The normalized draft data from the document
     * @param tenantId The tenant owning the document
     * @return true if the document can be auto-confirmed
     */
    suspend fun canAutoConfirm(
        source: DocumentSource,
        draftData: DocumentDraftData,
        tenantId: TenantId
    ): Boolean
}

/**
 * Default confirmation policy using simple source-based rules.
 *
 * - PEPPOL: Always auto-confirm (certified network, pre-validated UBL)
 * - MANUAL: Always auto-confirm (user entered data themselves)
 * - UPLOAD: Needs review (AI extraction may have errors)
 * - EMAIL: Needs review (AI extraction may have errors)
 */
class DefaultDocumentConfirmationPolicy : DocumentConfirmationPolicy {

    override suspend fun canAutoConfirm(
        source: DocumentSource,
        draftData: DocumentDraftData,
        tenantId: TenantId
    ): Boolean {
        return when (source) {
            DocumentSource.Peppol -> true // Certified network, always trust
            DocumentSource.Manual -> true // User entered it themselves
            DocumentSource.Upload -> false // Needs review (AI extraction)
            DocumentSource.Email -> false // Needs review (AI extraction)
        }
    }
}
