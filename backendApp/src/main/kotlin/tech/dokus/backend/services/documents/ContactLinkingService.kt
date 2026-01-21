package tech.dokus.backend.services.documents

import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.enums.ContactLinkDecisionType
import tech.dokus.domain.enums.ContactLinkPolicy
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.ContactEvidence
import tech.dokus.domain.model.ExtractedDocumentData

/**
 * Applies orchestrator contact linking decisions to drafts.
 */
class ContactLinkingService(
    private val draftRepository: DocumentDraftRepository,
    private val contactRepository: ContactRepository,
    private val linkingPolicy: ContactLinkPolicy
) {
    @Suppress("LongParameterList")
    suspend fun applyLinkDecision(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        extractedData: ExtractedDocumentData,
        decisionType: ContactLinkDecisionType?,
        contactId: ContactId,
        decisionReason: String?,
        decisionConfidence: Float?,
        evidence: ContactEvidence?
    ): Boolean {
        val draft = draftRepository.getByDocumentId(documentId, tenantId) ?: return false
        if (draft.linkedContactId != null) return false
        if (draft.counterpartyIntent == CounterpartyIntent.Pending) return false
        if (draft.draftVersion > 0) return false
        if (draft.linkedContactSource == ContactLinkSource.User) return false

        val contactVat = contactRepository.getContact(contactId, tenantId)
            .getOrNull()
            ?.vatNumber
            ?.value
            ?: return false

        val counterpartyVat = when (documentType) {
            DocumentType.Invoice -> extractedData.invoice?.clientVatNumber
            DocumentType.Bill -> extractedData.bill?.supplierVatNumber
            DocumentType.Expense -> extractedData.expense?.merchantVatNumber
            DocumentType.Receipt -> extractedData.receipt?.merchantVatNumber
            DocumentType.CreditNote -> extractedData.creditNote?.counterpartyVatNumber
            DocumentType.ProForma -> extractedData.proForma?.clientVatNumber
            DocumentType.Unknown -> null
        }

        val vatValid = counterpartyVat?.let { VatNumber(it).isValid } == true
        val vatMatched = vatValid &&
            counterpartyVat != null &&
            VatNumber.normalize(counterpartyVat) == VatNumber.normalize(contactVat)

        val mergedEvidence = (evidence ?: ContactEvidence()).copy(
            vatExtracted = evidence?.vatExtracted ?: counterpartyVat,
            vatValid = evidence?.vatValid ?: vatValid,
            vatMatched = evidence?.vatMatched ?: vatMatched,
            ambiguityCount = evidence?.ambiguityCount ?: 1
        )

        val effectiveDecision = ContactLinkDecisionResolver.resolve(
            policy = linkingPolicy,
            requested = decisionType,
            hasContact = true,
            vatMatched = vatMatched,
            evidence = mergedEvidence
        )

        return when (effectiveDecision) {
            ContactLinkDecisionType.AutoLink -> draftRepository.updateCounterparty(
                documentId = documentId,
                tenantId = tenantId,
                contactId = contactId,
                intent = CounterpartyIntent.None,
                source = ContactLinkSource.AI,
                contactEvidence = mergedEvidence
            )
            ContactLinkDecisionType.Suggest -> draftRepository.updateContactSuggestion(
                documentId = documentId,
                tenantId = tenantId,
                contactId = contactId,
                confidence = decisionConfidence,
                reason = decisionReason,
                contactEvidence = mergedEvidence
            )
            ContactLinkDecisionType.None -> false
        }
    }
}
