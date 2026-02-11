package tech.dokus.domain.model.contact

import kotlinx.serialization.Serializable
import tech.dokus.domain.Email
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus

@Serializable
data class CounterpartySnapshot(
    val name: String? = null,
    val vatNumber: VatNumber? = null,
    val iban: Iban? = null,
    val email: Email? = null,
    val companyNumber: String? = null,
    val streetLine1: String? = null,
    val streetLine2: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: Country? = null
)

@Serializable
data class ContactMatchScore(
    val vatMatch: Boolean,
    val ibanMatch: Boolean,
    val nameSimilarity: Double,
    val ambiguityCount: Int,
    val cbeResult: EntityLookup? = null
)

@Serializable
data class SuggestedContact(
    val contactId: ContactId,
    val name: String,
    val vatNumber: VatNumber? = null,
    val iban: Iban? = null,
    val matchScore: ContactMatchScore,
    val reason: String
)

@Serializable
data class MatchEvidence(
    val vatMatch: Boolean,
    val ibanMatch: Boolean,
    val nameSimilarity: Double? = null,
    val ambiguityCount: Int = 0,
    val cbeStatus: EntityStatus? = null
)

@Serializable
sealed class ContactResolution {
    @Serializable
    data class Matched(
        val contactId: ContactId,
        val evidence: MatchEvidence,
    ) : ContactResolution()

    @Serializable
    data class AutoCreate(
        val contactData: CounterpartySnapshot,
        val cbeVerified: EntityLookup?,
        val evidence: MatchEvidence,
    ) : ContactResolution()

    @Serializable
    data class Suggested(
        val candidates: List<SuggestedContact>,
        val suggestedNew: CounterpartySnapshot?,
        val reason: String,
    ) : ContactResolution()

    @Serializable
    data class PendingReview(
        val extractedData: CounterpartySnapshot,
    ) : ContactResolution()
}
