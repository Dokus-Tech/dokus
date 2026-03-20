package tech.dokus.backend.services.documents.resolution

import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.MatchEvidenceDto

class NameIbanAutoCreateResolver {
    suspend operator fun invoke(input: ResolverInput): ResolverOutcome {
        val snapshot = input.snapshot
        if (snapshot.vatNumber != null || snapshot.name == null || snapshot.iban == null) return ResolverOutcome.Partial()

        return ResolverOutcome.Resolved(
            ContactResolution.AutoCreate(
                contactData = snapshot,
                cbeVerified = null,
                evidence = noMatchEvidence
            )
        )
    }
}

private val noMatchEvidence = MatchEvidenceDto(
    vatMatch = false,
    ibanMatch = false,
    nameSimilarity = null,
    ambiguityCount = 0,
    cbeStatus = null
)
