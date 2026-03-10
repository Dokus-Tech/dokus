package tech.dokus.domain.model.contact

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.ids.ContactId

@Serializable
sealed interface CounterpartyInfo {
    @Serializable
    @SerialName("unresolved")
    data class Unresolved(
        val snapshot: CounterpartySnapshot? = null,
        val suggestions: List<SuggestedContact> = emptyList(),
        val pendingCreation: Boolean = false,
    ) : CounterpartyInfo

    @Serializable
    @SerialName("linked")
    data class Linked(
        val contactId: ContactId,
        val source: ContactLinkSource,
        val evidence: MatchEvidence? = null,
    ) : CounterpartyInfo
}
