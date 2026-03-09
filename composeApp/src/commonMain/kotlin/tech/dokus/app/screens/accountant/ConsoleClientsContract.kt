package tech.dokus.app.screens.accountant

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess

@Immutable
data class ConsoleClientsState(
    val firmId: FirmId? = null,
    val firmName: String? = null,
    val clients: DokusState<List<ConsoleClientSummary>> = DokusState.loading(),
    val query: String = "",
    val selectedClientTenantId: TenantId? = null,
    val documentsState: DokusState<List<DocumentRecordDto>> = DokusState.idle(),
    val selectedDocument: DocumentRecordDto? = null,
    val loadingDocumentId: String? = null,
) : MVIState {
    val filteredClients: List<ConsoleClientSummary>
        get() {
            if (!clients.isSuccess()) return emptyList()
            val allClients = clients.data
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) return allClients
            return allClients.filter { client ->
                client.companyName.value.contains(normalizedQuery, ignoreCase = true) ||
                    (client.vatNumber?.value?.contains(normalizedQuery, ignoreCase = true) == true)
            }
        }
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
