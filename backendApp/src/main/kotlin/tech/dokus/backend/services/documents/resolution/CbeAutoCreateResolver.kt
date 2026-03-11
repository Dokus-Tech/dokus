package tech.dokus.backend.services.documents.resolution

import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.PostalAddress
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.foundation.backend.lookup.CbeApiClient

class CbeAutoCreateResolver(
    private val cbeApiClient: CbeApiClient
) {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val snapshot = input.snapshot
        val vat = snapshot.vatNumber
        if (vat == null || !vat.isValid || !vat.isBelgian) return ResolverOutcome.Partial()

        val cbeLookup = cbeApiClient.searchByVat(vat).getOrNull() ?: return ResolverOutcome.Partial()

        return when (cbeLookup.status) {
            EntityStatus.Active -> ResolverOutcome.Resolved(
                ContactResolution.AutoCreate(
                    contactData = snapshotFromCbe(cbeLookup, snapshot),
                    cbeVerified = cbeLookup,
                    evidence = MatchEvidence(
                        vatMatch = false,
                        ibanMatch = false,
                        nameSimilarity = null,
                        ambiguityCount = 0,
                        cbeStatus = EntityStatus.Active
                    )
                )
            )
            EntityStatus.Inactive -> ResolverOutcome.Resolved(
                ContactResolution.PendingReview(snapshot)
            )
            EntityStatus.Unknown -> ResolverOutcome.Partial()
        }
    }

    private fun snapshotFromCbe(
        lookup: EntityLookup,
        fallback: CounterpartySnapshot
    ): CounterpartySnapshot {
        val cbeAddress = lookup.address
        return CounterpartySnapshot(
            name = lookup.name.value,
            vatNumber = lookup.vatNumber,
            iban = fallback.iban,
            email = fallback.email,
            companyNumber = lookup.enterpriseNumber,
            address = PostalAddress(
                streetLine1 = cbeAddress?.streetLine1,
                streetLine2 = cbeAddress?.streetLine2,
                postalCode = cbeAddress?.postalCode,
                city = cbeAddress?.city,
                country = cbeAddress?.country
            )
        )
    }
}
