package ai.dokus.app.app.home.splash

import ai.dokus.app.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.platform.persistence
import ai.dokus.app.repository.extensions.authCredentials
import ai.dokus.app.repository.extensions.user
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