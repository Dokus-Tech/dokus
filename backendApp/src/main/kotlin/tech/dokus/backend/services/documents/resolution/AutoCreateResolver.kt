package tech.dokus.backend.services.documents.resolution

import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.PostalAddress
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.foundation.backend.lookup.CbeApiClient

class AutoCreateResolver(
    private val cbeApiClient: CbeApiClient
) {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val snapshot = input.snapshot
        val vat = snapshot.vatNumber
        val name = snapshot.name
        val iban = snapshot.iban

        // 3) CBE auto-create for Belgian VAT
        if (vat != null && vat.isValid && vat.isBelgian) {
            val cbeLookup = cbeApiClient.searchByVat(vat).getOrNull()
            if (cbeLookup != null) {
                when (cbeLookup.status) {
                    EntityStatus.Active -> return ResolverOutcome.Resolved(
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
                    EntityStatus.Inactive -> return ResolverOutcome.Resolved(
                        ContactResolution.PendingReview(snapshot)
                    )
                    EntityStatus.Unknown -> {
                        // Fall through to non-CBE logic
                    }
                }
            }
        }

        // 4) Auto-create for valid non-BE VAT
        if (vat != null && vat.isValid && !vat.isBelgian && name != null) {
            return ResolverOutcome.Resolved(
                ContactResolution.AutoCreate(
                    contactData = snapshot,
                    cbeVerified = null,
                    evidence = noMatchEvidence
                )
            )
        }

        // 5) Auto-create without VAT if name + IBAN present
        if (vat == null && name != null && iban != null) {
            return ResolverOutcome.Resolved(
                ContactResolution.AutoCreate(
                    contactData = snapshot,
                    cbeVerified = null,
                    evidence = noMatchEvidence
                )
            )
        }

        return ResolverOutcome.Partial()
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

    companion object {
        private val noMatchEvidence = MatchEvidence(
            vatMatch = false,
            ibanMatch = false,
            nameSimilarity = null,
            ambiguityCount = 0,
            cbeStatus = null
        )
    }
}
