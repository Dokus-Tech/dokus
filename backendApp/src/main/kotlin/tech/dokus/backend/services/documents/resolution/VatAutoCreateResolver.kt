package tech.dokus.backend.services.documents.resolution

import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.MatchEvidence

class VatAutoCreateResolver {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val snapshot = input.snapshot
        val vat = snapshot.vatNumber
        if (vat == null || !vat.isValid || vat.isBelgian || snapshot.name == null) return ResolverOutcome.Partial()

        return ResolverOutcome.Resolved(
            ContactResolution.AutoCreate(
                contactData = snapshot,
                cbeVerified = null,
                evidence = noMatchEvidence
            )
        )
    }
}

private val noMatchEvidence = MatchEvidence(
    vatMatch = false,
    ibanMatch = false,
    nameSimilarity = null,
    ambiguityCount = 0,
    cbeStatus = null
)
