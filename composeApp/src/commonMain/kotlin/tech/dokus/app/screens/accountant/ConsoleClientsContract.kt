package tech.dokus.app.screens.accountant

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.foundation.app.state.DokusState

@Immutable
sealed interface ConsoleClientsState : MVIState, DokusState<Nothing> {

    data object Loading : ConsoleClientsState

    data class Content(
        val firmId: FirmId,
        val clients: List<ConsoleClientSummary>,
        val query: String = "",
        val selectedClientTenantId: TenantId? = null,
        val documentsState: DokusState<List<DocumentRecordDto>> = DokusState.idle(),
        val selectedDocument: DocumentRecordDto? = null,
        val loadingDocumentId: String? = null,
    ) : ConsoleClientsState {
        val filteredClients: List<ConsoleClientSummary>
            get() {
                val normalizedQuery = query.trim()
                if (normalizedQuery.isEmpty()) return clients
                return clients.filter { client ->
                    client.companyName.value.contains(normalizedQuery, ignoreCase = true) ||
                        (client.vatNumber?.value?.contains(normalizedQuery, ignoreCase = true) == true)
                }
            }
    }

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ConsoleClientsState, DokusState.Error<Nothing>
}

@Immutable
sealed interface ConsoleClientsIntent : MVIIntent {
    data object Refresh : ConsoleClientsIntent
    data class UpdateQuery(val query: String) : ConsoleClientsIntent
    data class SelectClient(val tenantId: TenantId) : ConsoleClientsIntent
    data object BackToClients : ConsoleClientsIntent
    data class OpenDocument(val documentId: String) : ConsoleClientsIntent
}

@Immutable
sealed interface ConsoleClientsAction : MVIAction {
    data class ShowError(val error: DokusException) : ConsoleClientsAction
}
