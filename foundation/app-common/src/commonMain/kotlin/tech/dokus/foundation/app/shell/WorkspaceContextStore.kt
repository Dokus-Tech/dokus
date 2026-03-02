package tech.dokus.foundation.app.shell

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.dokus.domain.ids.FirmId
import tech.dokus.foundation.app.NavContext

/**
 * In-memory workspace context used by the shell to decide whether to render
 * tenant (business) or firm (console) navigation.
 */
data class WorkspaceContext(
    val navContext: NavContext = NavContext.TENANT,
    val selectedFirmId: FirmId? = null,
)

object WorkspaceContextStore {
    private val mutableState = MutableStateFlow(WorkspaceContext())
    val state: StateFlow<WorkspaceContext> = mutableState.asStateFlow()

    fun selectTenantWorkspace() {
        mutableState.value = WorkspaceContext(navContext = NavContext.TENANT, selectedFirmId = null)
    }

    fun switchToTenantWorkspace() {
        mutableState.value = mutableState.value.copy(navContext = NavContext.TENANT)
    }

    fun switchToFirmWorkspace() {
        mutableState.value = mutableState.value.copy(navContext = NavContext.FIRM)
    }

    fun selectFirmWorkspace(firmId: FirmId) {
        mutableState.value = WorkspaceContext(navContext = NavContext.FIRM, selectedFirmId = firmId)
    }
}
