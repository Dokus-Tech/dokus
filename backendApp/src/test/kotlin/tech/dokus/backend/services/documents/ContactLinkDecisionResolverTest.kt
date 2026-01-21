package tech.dokus.backend.services.documents

import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.ContactLinkDecisionType
import tech.dokus.domain.enums.ContactLinkPolicy
import tech.dokus.domain.model.ContactEvidence
import kotlin.test.assertEquals

class ContactLinkDecisionResolverTest {

    @Test
    fun `VAT_ONLY auto-links only on VAT match`() {
        val evidence = ContactEvidence(ambiguityCount = 1)
        val decision = ContactLinkDecisionResolver.resolve(
            policy = ContactLinkPolicy.VatOnly,
            requested = ContactLinkDecisionType.AutoLink,
            hasContact = true,
            vatMatched = true,
            evidence = evidence
        )

        assertEquals(ContactLinkDecisionType.AutoLink, decision)
    }

    @Test
    fun `VAT_ONLY downgrades AUTO_LINK when VAT does not match`() {
        val evidence = ContactEvidence(ambiguityCount = 1)
        val decision = ContactLinkDecisionResolver.resolve(
            policy = ContactLinkPolicy.VatOnly,
            requested = ContactLinkDecisionType.AutoLink,
            hasContact = true,
            vatMatched = false,
            evidence = evidence
        )

        assertEquals(ContactLinkDecisionType.Suggest, decision)
    }

    @Test
    fun `VAT_OR_STRONG_SIGNALS allows auto-link on strong evidence`() {
        val evidence = ContactEvidence(
            ibanMatched = true,
            addressMatched = true,
            nameSimilarity = 0.93,
            ambiguityCount = 1
        )
        val decision = ContactLinkDecisionResolver.resolve(
            policy = ContactLinkPolicy.VatOrStrongSignals,
            requested = ContactLinkDecisionType.AutoLink,
            hasContact = true,
            vatMatched = false,
            evidence = evidence
        )

        assertEquals(ContactLinkDecisionType.AutoLink, decision)
    }
}
