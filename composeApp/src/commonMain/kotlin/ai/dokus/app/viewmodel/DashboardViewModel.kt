package ai.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentOrganizationUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.model.Organization
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DashboardViewModel(
    private val getCurrentOrganizationUseCase: GetCurrentOrganizationUseCase
) : ViewModel() {
    private val mutableCurrentOrganizationState = MutableStateFlow<DokusState<Organization?>>(DokusState.idle())
    val currentOrganizationState = mutableCurrentOrganizationState.asStateFlow()

    fun refreshOrganization() {
        viewModelScope.launch {
            mutableCurrentOrganizationState.value = DokusState.loading()

            val nextState = getCurrentOrganizationUseCase().fold(
                onSuccess = { organization ->
                    DokusState.success(organization)
                },
                onFailure = {
                    DokusState.error(it) { refreshOrganization() }
                }
            )

            mutableCurrentOrganizationState.value = nextState
        }
    }
}
