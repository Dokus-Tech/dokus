package ai.dokus.app.viewmodel

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.core.viewmodel.BaseViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val authInitializer: AuthInitializer,
//    private val userRepository: UserRepository,
) : BaseViewModel<List<BootstrapViewModel.BootstrapState>>(BootstrapState.all) {
    private val mutableEffect = MutableStateFlow<Effect>(Effect.Idle)
    val effect: StateFlow<Effect> = mutableEffect.asStateFlow()

    fun load() {
        viewModelScope.launch {
            mutableEffect.value = when {
                needsUpdate() -> Effect.NeedsUpdate
                needsLogin() -> Effect.NeedsLogin
                needsAccountConfirmation() -> Effect.NeedsAccountConfirmation
                else -> Effect.Ok
            }
        }
    }

    private suspend fun needsLogin(): Boolean {
        updateStep(BootstrapState.CheckingLogin(true))
        authInitializer.initialize()
        // Check if user is authenticated
        return !authInitializer.isAuthenticated()
    }

    private suspend fun needsAccountConfirmation(): Boolean {
        updateStep(BootstrapState.CheckingAccountStatus(true))
        return false
//        // Check if user is active - fetches from network and updates local database
//        val user = userRepository.fetchCurrentUser().getOrElse {
//            return false // TODO: Handle cases when there is no internet
//        }
//        return user.status == UserStatus.PENDING_VERIFICATION
    }

    private fun needsUpdate(): Boolean {
        updateStep(BootstrapState.CheckUpdate(true))
        return false
    }

    private fun updateStep(step: BootstrapState) {
        mutableState.value = state.value.map {
            if (it == step) {
                step
            } else {
                it
            }
        }
    }

    sealed interface BootstrapState {
        val isActive: Boolean

        data class InitializeApp(override val isActive: Boolean) : BootstrapState
        data class CheckingLogin(override val isActive: Boolean) : BootstrapState
        data class CheckingAccountStatus(override val isActive: Boolean) : BootstrapState
        data class CheckUpdate(override val isActive: Boolean) : BootstrapState

        companion object {
            val all = listOf<BootstrapState>(
                InitializeApp(isActive = false),
                CheckUpdate(isActive = false),
                CheckingLogin(isActive = false),
                CheckingAccountStatus(isActive = false),
            )
        }
    }

    sealed interface Effect {
        data object Idle : Effect
        data object NeedsLogin : Effect
        data object NeedsUpdate : Effect
        data object NeedsAccountConfirmation : Effect
        data object Ok : Effect
    }
}