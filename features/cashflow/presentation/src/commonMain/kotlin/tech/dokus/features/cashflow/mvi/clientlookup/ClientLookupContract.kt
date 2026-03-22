package tech.dokus.features.cashflow.mvi.clientlookup

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.model.ClientLookupState
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion

@Immutable
data class ClientLookupChildState(
    val clientLookupState: ClientLookupState = ClientLookupState(),
) : MVIState

@Immutable
sealed interface ClientLookupIntent : MVIIntent {
    data class UpdateQuery(val query: String) : ClientLookupIntent
    data class SetExpanded(val expanded: Boolean) : ClientLookupIntent
    data class SelectClient(val client: ContactDto) : ClientLookupIntent
    data class SelectExternal(val candidate: ExternalClientCandidate) : ClientLookupIntent
    data class CreateManually(val query: String) : ClientLookupIntent
    data object Clear : ClientLookupIntent
    data class RefreshPeppolStatus(val contactId: ContactId, val force: Boolean = false) : ClientLookupIntent
}

@Immutable
sealed interface ClientLookupAction : MVIAction {
    data class ClientSelected(
        val contact: ContactDto,
        val peppolStatus: PeppolStatusResponse? = null,
        val latestInvoiceSuggestion: LatestInvoiceSuggestion? = null,
        val senderIban: String? = null,
    ) : ClientLookupAction

    data object ClientCleared : ClientLookupAction

    data class NavigateToCreateContact(
        val prefillCompanyName: String? = null,
        val prefillVat: String? = null,
        val prefillAddress: String? = null,
        val origin: String? = null,
    ) : ClientLookupAction

    data class PeppolStatusUpdated(
        val contactId: ContactId,
        val peppolStatus: PeppolStatusResponse?,
    ) : ClientLookupAction
}
