package tech.dokus.backend.services.documents.resolution

import tech.dokus.backend.services.documents.resolution.ContactMatchingUtils.Companion.SuggestionThreshold
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.MatchEvidence

class NameSuggestionResolver(
    private val contactRepository: ContactRepository,
    private val matchingUtils: ContactMatchingUtils
) {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val name = input.snapshot.name ?: return ResolverOutcome.Partial()

        val candidates = contactRepository.findByName(input.tenantId, name, limit = 10).getOrNull().orEmpty()
        val scored = candidates.map { contact ->
            contact to matchingUtils.similarity(name, contact.name.value)
        }.filter { it.second >= SuggestionThreshold }

        // Auto-link on exact unambiguous name match (unless direction is Unknown)
        if (!input.strictAutoLink && scored.size == 1 && scored.first().second >= 1.0) {
            val (contact, score) = scored.first()
            val healedContact = matchingUtils.maybeHealPaymentAliasContact(
                tenantId = input.tenantId,
                matchedContact = contact,
                authoritativeName = name
            )
            return ResolverOutcome.Resolved(
                ContactResolution.Matched(
                    contactId = healedContact.id,
                    evidence = MatchEvidence(
                        vatMatch = false,
                        ibanMatch = false,
                        nameSimilarity = score,
                        ambiguityCount = 1,
                        cbeStatus = null
                    )
                )
            )
        }

        if (scored.isEmpty()) return ResolverOutcome.Partial()

        val ambiguityCount = scored.size
        return with(matchingUtils) {
            ResolverOutcome.Partial(
                scored.map { (contact, score) ->
                    contact.toSuggestedContact(
                        vatMatch = false,
                        ibanMatch = false,
                        nameSimilarity = score,
                        ambiguityCount = ambiguityCount,
                        reason = "Name similarity ${formatScore(score)}"
                    )
                }
            )
        }
    }
}
