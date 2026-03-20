package tech.dokus.domain.model.contact

import kotlinx.serialization.Serializable
import tech.dokus.domain.Email
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus

@Serializable
data class PostalAddressDto(
    val streetLine1: String? = null,
    val streetLine2: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: Country? = null,
)

@Serializable
data class CounterpartySnapshotDto(
    val name: String? = null,
    val vatNumber: VatNumber? = null,
    val iban: Iban? = null,
    val bic: Bic? = null,
    val email: Email? = null,
    val companyNumber: String? = null,
    val address: PostalAddressDto = PostalAddressDto(),
)

@Serializable
data class ContactMatchScoreDto(
    val vatMatch: Boolean,
    val ibanMatch: Boolean,
    val nameSimilarity: Double,
    val ambiguityCount: Int,
    val cbeResult: EntityLookup? = null
)

@Serializable
data class SuggestedContactDto(
    val contactId: ContactId,
    val name: String,
    val vatNumber: VatNumber? = null,
    val iban: Iban? = null,
    val matchScore: ContactMatchScoreDto,
    val reason: String
)

@Serializable
data class MatchEvidenceDto(
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
        val evidence: MatchEvidenceDto,
    ) : ContactResolution()

    @Serializable
    data class AutoCreate(
        val contactData: CounterpartySnapshotDto,
        val cbeVerified: EntityLookup?,
        val evidence: MatchEvidenceDto,
    ) : ContactResolution()

    @Serializable
    data class Suggested(
        val candidates: List<SuggestedContactDto>,
        val suggestedNew: CounterpartySnapshotDto?,
        val reason: String,
    ) : ContactResolution()

    @Serializable
    data class PendingReview(
        val extractedData: CounterpartySnapshotDto,
    ) : ContactResolution()
}
