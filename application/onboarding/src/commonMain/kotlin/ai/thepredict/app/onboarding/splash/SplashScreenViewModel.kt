package ai.thepredict.app.onboarding.splash

import ai.thepredict.app.platform.persistence
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

class SplashScreenViewModel : StateScreenModel<SplashScreenViewModel.Effect>(Effect.Idle) {

    fun checkOnboarding() {
        screenModelScope.launch {
            val isUserLoggedIn = persistence.email != null && persistence.password != null

            if (isUserLoggedIn) {
                mutableState.value = Effect.NavigateHome
            } else {
                mutableState.value = Effect.NavigateToLogin
            }
        }
    }

    sealed interface Effect {
        data object Idle : Effect

        data object NavigateToLogin : Effect

        data object NavigateHome : Effect
    }
}