package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.processing.DocumentProcessingConstants

class AutoConfirmPolicy(
    private val documentRepository: DocumentRepository
) {
    suspend fun canAutoConfirm(
        tenantId: TenantId,
        documentId: DocumentId,
        source: DocumentSource,
        documentType: DocumentType,
        draftData: DocumentDraftData,
        auditPassed: Boolean,
        confidence: Double,
        linkedContactId: ContactId?,
        directionResolvedFromAiHintOnly: Boolean
    ): Boolean {
        if (source == DocumentSource.Manual) return false

        val draftType = draftData.toDocumentType()
        if (documentType == DocumentType.Unknown || draftType != documentType) return false
        if (draftData is InvoiceDraftData && linkedContactId == null) return false
        if (draftData is CreditNoteDraftData && linkedContactId == null) return false
        if (directionResolvedFromAiHintOnly && draftData.requiresDirection()) return false
        if (!isDirectionValid(draftData)) return false
        if (!isAmountPositive(draftData)) return false
        if (!auditPassed) return false
        if (isDuplicate(tenantId, documentId)) return false

        return when (source) {
            DocumentSource.Peppol -> true
            DocumentSource.Upload,
            DocumentSource.Email -> {
                val meetsConfidence = confidence >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
                val counterpartyKnown = linkedContactId != null
                meetsConfidence && counterpartyKnown
            }
            DocumentSource.Manual -> false
        }
    }

    private suspend fun isDuplicate(tenantId: TenantId, documentId: DocumentId): Boolean {
        val contentHash = documentRepository.getContentHash(tenantId, documentId) ?: return false
        val existing = documentRepository.getByContentHash(tenantId, contentHash) ?: return false
        return existing.id != documentId
    }

    private fun isDirectionValid(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.direction != DocumentDirection.Unknown
            is ReceiptDraftData -> draftData.direction != DocumentDirection.Unknown
            is CreditNoteDraftData -> draftData.direction != DocumentDirection.Unknown
        }
    }

    private fun isAmountPositive(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.totalAmount?.isPositive == true
            is ReceiptDraftData -> draftData.totalAmount?.isPositive == true
            is CreditNoteDraftData -> draftData.totalAmount?.isPositive == true
        }
    }

    private fun DocumentDraftData.toDocumentType(): DocumentType = when (this) {
        is InvoiceDraftData -> DocumentType.Invoice
        is ReceiptDraftData -> DocumentType.Receipt
        is CreditNoteDraftData -> DocumentType.CreditNote
    }

    private fun DocumentDraftData.requiresDirection(): Boolean = when (this) {
        is InvoiceDraftData -> true
        is ReceiptDraftData -> true
        is CreditNoteDraftData -> true
    }
}
