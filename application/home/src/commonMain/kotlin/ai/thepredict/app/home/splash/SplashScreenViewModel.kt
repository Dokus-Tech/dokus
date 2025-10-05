package ai.thepredict.app.home.splash

import ai.thepredict.app.core.viewmodel.BaseViewModel
import ai.thepredict.app.platform.persistence
import ai.thepredict.repository.extensions.authCredentials
import ai.thepredict.repository.extensions.user
import kotlinx.coroutines.launch

internal class SplashScreenViewModel : BaseViewModel<SplashScreenViewModel.Effect>(Effect.Idle) {

    fun checkOnboarding() {
        scope.launch {
            val effect = when {
                checkIsNotLoggedIn() -> Effect.NavigateToLogin
                noWorkspaceIsSelected() -> Effect.NavigateToWorkspacesOverview
                else -> Effect.NavigateHome
            }
            mutableState.value = effect
        }
    }

    private fun checkIsNotLoggedIn(): Boolean {
        return persistence.authCredentials == null || persistence.user == null
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