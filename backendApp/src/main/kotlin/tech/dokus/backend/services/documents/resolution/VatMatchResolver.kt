package tech.dokus.backend.services.documents.resolution

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.foundation.backend.lookup.CbeApiClient

class VatMatchResolver(
    private val contactRepository: ContactRepository,
    private val cbeApiClient: CbeApiClient,
    private val matchingUtils: ContactMatchingUtils
) {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val vat = input.snapshot.vatNumber ?: return ResolverOutcome.Partial()
        val name = input.snapshot.name

        val vatMatch = contactRepository.findByVatNumber(input.tenantId, vat.value).getOrNull()
            ?: return ResolverOutcome.Partial()

        val cbeStatus = resolveCbeStatus(vat)
        val allowAutoLink = !input.strictAutoLink || cbeStatus == EntityStatus.Active

        if (allowAutoLink) {
            val healedContact = matchingUtils.maybeHealPaymentAliasContact(
                tenantId = input.tenantId,
                matchedContact = vatMatch,
                authoritativeName = name
            )
            val nameSimilarity = name?.let { matchingUtils.similarity(it, healedContact.name.value) }
            val evidence = MatchEvidence(
                vatMatch = true,
                ibanMatch = false,
                nameSimilarity = nameSimilarity,
                ambiguityCount = 1,
                cbeStatus = cbeStatus
            )
            return ResolverOutcome.Resolved(
                ContactResolution.Matched(
                    contactId = healedContact.id,
                    evidence = evidence
                )
            )
        }

        val nameSimilarity = name?.let { matchingUtils.similarity(it, vatMatch.name.value) }
        return with(matchingUtils) {
            ResolverOutcome.Partial(
                listOf(
                    vatMatch.toSuggestedContact(
                        vatMatch = true,
                        ibanMatch = false,
                        nameSimilarity = nameSimilarity,
                        ambiguityCount = 1,
                        reason = "VAT matched but direction is unclear"
                    )
                )
            )
        }
    }

    private suspend fun resolveCbeStatus(vat: VatNumber): EntityStatus? {
        if (!vat.isValid || !vat.isBelgian) return null
        return cbeApiClient.searchByVat(vat).getOrNull()?.status
    }
}
