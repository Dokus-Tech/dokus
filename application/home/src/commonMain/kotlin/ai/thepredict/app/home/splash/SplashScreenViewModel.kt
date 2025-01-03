package ai.thepredict.app.home.splash

import ai.thepredict.app.platform.persistence
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

internal class SplashScreenViewModel : StateScreenModel<SplashScreenViewModel.Effect>(Effect.Idle) {

    fun checkOnboarding() {
        screenModelScope.launch {
            val effect = when {
                checkIsNotLoggedIn() -> Effect.NavigateToLogin
                noWorkspaceIsSelected() -> Effect.NavigateToWorkspacesOverview
                else -> Effect.NavigateHome
            }
            mutableState.value = effect
        }
    }

    private fun checkIsNotLoggedIn(): Boolean {
        return persistence.email == null || persistence.password == null
    }

    private fun noWorkspaceIsSelected(): Boolean {
        return persistence.selectedWorkspace == null
    }

    sealed interface Effect {
        data object Idle : Effect

        data object NavigateToLogin : Effect

        data object NavigateToWorkspacesOverview : Effect

        data object NavigateHome : Effect
    }
}