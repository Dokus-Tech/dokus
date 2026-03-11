package tech.dokus.backend.services.documents.resolution

import tech.dokus.backend.services.documents.resolution.ContactMatchingUtils.Companion.StrongNameThreshold
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.MatchEvidence

class IbanNameResolver(
    private val contactRepository: ContactRepository,
    private val matchingUtils: ContactMatchingUtils
) {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val iban = input.snapshot.iban ?: return ResolverOutcome.Partial()
        val name = input.snapshot.name ?: return ResolverOutcome.Partial()

        val ibanMatches = contactRepository.findByIban(input.tenantId, iban).getOrNull().orEmpty()
        if (ibanMatches.isEmpty()) return ResolverOutcome.Partial()

        val scored = ibanMatches.map { contact ->
            contact to matchingUtils.similarity(name, contact.name.value)
        }
        val strongMatches = scored.filter { it.second >= StrongNameThreshold }

        if (strongMatches.size == 1 && (!input.strictAutoLink || strongMatches.first().second >= StrongNameThreshold)) {
            val (contact, score) = strongMatches.first()
            val evidence = MatchEvidence(
                vatMatch = false,
                ibanMatch = true,
                nameSimilarity = score,
                ambiguityCount = strongMatches.size,
                cbeStatus = null
            )
            return ResolverOutcome.Resolved(
                ContactResolution.Matched(
                    contactId = contact.id,
                    evidence = evidence
                )
            )
        }

        if (strongMatches.isEmpty()) return ResolverOutcome.Partial()

        val ambiguityCount = strongMatches.size
        return with(matchingUtils) {
            ResolverOutcome.Partial(
                strongMatches.map { (contact, score) ->
                    contact.toSuggestedContact(
                        vatMatch = false,
                        ibanMatch = true,
                        nameSimilarity = score,
                        ambiguityCount = ambiguityCount,
                        reason = "IBAN match with name similarity ${formatScore(score)}"
                    )
                }
            )
        }
    }
}
