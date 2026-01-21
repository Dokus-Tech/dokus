package tech.dokus.backend.services.documents

import tech.dokus.domain.enums.ContactLinkDecisionType
import tech.dokus.domain.enums.ContactLinkPolicy
import tech.dokus.domain.model.ContactEvidence

/**
 * Centralized decision resolver for contact linking in document processing.
 *
 * This keeps storage-side linking consistent with orchestrator decisions
 * while enforcing safety gates (VAT-only by default).
 */
object ContactLinkDecisionResolver {

    private const val StrongNameSimilarityThreshold = 0.93

    @Suppress("CyclomaticComplexMethod")
    fun resolve(
        policy: ContactLinkPolicy,
        requested: ContactLinkDecisionType?,
        hasContact: Boolean,
        vatMatched: Boolean,
        evidence: ContactEvidence
    ): ContactLinkDecisionType {
        val ambiguityCount = evidence.ambiguityCount ?: 1
        val strongSignals = policy == ContactLinkPolicy.VatOrStrongSignals &&
            evidence.ibanMatched == true &&
            evidence.addressMatched == true &&
            (evidence.nameSimilarity ?: 0.0) >= StrongNameSimilarityThreshold &&
            ambiguityCount == 1

        val autoLinkAllowed = (vatMatched && ambiguityCount == 1) || strongSignals

        return when (requested) {
            ContactLinkDecisionType.AutoLink ->
                if (autoLinkAllowed) ContactLinkDecisionType.AutoLink else ContactLinkDecisionType.Suggest
            ContactLinkDecisionType.Suggest -> ContactLinkDecisionType.Suggest
            ContactLinkDecisionType.None -> ContactLinkDecisionType.None
            null -> if (hasContact) ContactLinkDecisionType.Suggest else ContactLinkDecisionType.None
        }
    }
}
