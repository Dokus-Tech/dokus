package ai.dokus.app.viewmodel

import ai.dokus.app.auth.AuthInitializer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val authInitializer: AuthInitializer,
//    private val userRepository: UserRepository,
) : ViewModel() {
    private val mutableEffect = MutableStateFlow<Effect>(Effect.Idle)
    val effect: StateFlow<Effect> = mutableEffect.asStateFlow()

    private val mutableBootstrapState = MutableStateFlow(listOf(BootstrapState.InitializeApp))
    val loadingState = mutableBootstrapState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            // Initialize authentication system
            initializeAuth()

            mutableEffect.value = when {
                needsUpdate() -> Effect.NeedsUpdate
                needsLogin() -> Effect.NeedsLogin
                needsAccountConfirmation() -> Effect.NeedsAccountConfirmation
                else -> Effect.Ok
            }
        }
    }

    private suspend fun initializeAuth() {
        authInitializer.initialize()
    }

    private suspend fun needsLogin(): Boolean {
        mutableBootstrapState.value += BootstrapState.CheckingLogin
        // Check if user is authenticated
        return !authInitializer.isAuthenticated()
    }

    private suspend fun needsAccountConfirmation(): Boolean {
        mutableBootstrapState.value += BootstrapState.CheckingAccountStatus
        return false
//        // Check if user is active - fetches from network and updates local database
//        val user = userRepository.fetchCurrentUser().getOrElse {
//            return false // TODO: Handle cases when there is no internet
//        }
//        return user.status == UserStatus.PENDING_VERIFICATION
    }

    private fun needsUpdate(): Boolean {
        mutableBootstrapState.value += BootstrapState.CheckUpdate
        return false
    }

    enum class BootstrapState {
        InitializeApp,
        CheckingLogin,
        CheckingAccountStatus,
        CheckUpdate,
    }

    sealed interface Effect {
        data object Idle : Effect
        data object NeedsLogin : Effect
        data object NeedsUpdate : Effect
        data object NeedsAccountConfirmation : Effect
        data object Ok : Effect
    }
}