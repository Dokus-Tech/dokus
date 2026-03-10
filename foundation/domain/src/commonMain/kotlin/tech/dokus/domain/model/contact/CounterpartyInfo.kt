package tech.dokus.domain.model.contact

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.ids.ContactId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Serializable
sealed interface CounterpartyInfo {
    @Serializable
    @SerialName("CounterpartyInfo.Unresolved")
    data class Unresolved(
        val snapshot: CounterpartySnapshot? = null,
        val suggestions: List<SuggestedContact> = emptyList(),
        val pendingCreation: Boolean = false,
    ) : CounterpartyInfo

    @Serializable
    @SerialName("CounterpartyInfo.Linked")
    data class Linked(
        val contactId: ContactId,
        val source: ContactLinkSource,
        val evidence: MatchEvidence? = null,
    ) : CounterpartyInfo
}

@OptIn(ExperimentalContracts::class)
fun CounterpartyInfo?.isUnresolved(): Boolean {
    contract {
        returns(true) implies (this@isUnresolved is CounterpartyInfo.Unresolved)
    }
    return this is CounterpartyInfo.Unresolved
}

@OptIn(ExperimentalContracts::class)
fun CounterpartyInfo?.isLinked(): Boolean {
    contract {
        returns(true) implies (this@isLinked is CounterpartyInfo.Linked)
    }
    return this is CounterpartyInfo.Linked
}