package tech.dokus.app.viewmodel

import ai.dokus.app.auth.AuthInitializer
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.ServerConfigManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val authInitializer: AuthInitializer,
    private val tokenManager: TokenManager,
    private val serverConfigManager: ServerConfigManager,
) : BaseViewModel<List<BootstrapViewModel.BootstrapState>>(BootstrapState.all) {
    private val mutableEffect = MutableStateFlow<Effect>(Effect.Idle)
    val effect: StateFlow<Effect> = mutableEffect.asStateFlow()

    fun load() {
        viewModelScope.launch {
            // Initialize server config first (loads persisted server selection)
            initializeServerConfig()

            mutableEffect.value = when {
                needsUpdate() -> Effect.NeedsUpdate
                needsLogin() -> Effect.NeedsLogin
                needsAccountConfirmation() -> Effect.NeedsAccountConfirmation
                needsTenantSelection() -> Effect.NeedsTenantSelection
                else -> Effect.Ok
            }
        }
    }

    private suspend fun initializeServerConfig() {
        updateStep(BootstrapState.InitializeApp(isActive = true, isCurrent = true))
        serverConfigManager.initialize()
    }

    private suspend fun needsLogin(): Boolean {
        updateStep(BootstrapState.CheckingLogin(isActive = true, isCurrent = true))
        authInitializer.initialize()
        return !authInitializer.isAuthenticated()
    }

    private suspend fun needsAccountConfirmation(): Boolean {
        updateStep(BootstrapState.CheckingAccountStatus(isActive = true, isCurrent = true))
        return false
//        // Check if user is active - fetches from network and updates local database
//        val user = userRepository.fetchCurrentUser().getOrElse {
//            return false // TODO: Handle cases when there is no internet
//        }
//        return user.status == UserStatus.PENDING_VERIFICATION
    }

    private fun needsUpdate(): Boolean {
        updateStep(BootstrapState.CheckUpdate(isActive = true, isCurrent = true))
        return false
    }

    private suspend fun needsTenantSelection(): Boolean {
        val claims = tokenManager.getCurrentClaims()
        return claims?.tenant == null
    }

    private fun updateStep(step: BootstrapState) {
        mutableState.value = state.value.map {
            if (it == step) {
                step
            } else {
                it.copyCurrent(isCurrent = false)
            }
        }
    }

    sealed interface BootstrapState {
        val isActive: Boolean
        val isCurrent: Boolean

        data class InitializeApp(
            override val isActive: Boolean,
            override val isCurrent: Boolean
        ) : BootstrapState

        data class CheckingLogin(
            override val isActive: Boolean,
            override val isCurrent: Boolean
        ) : BootstrapState

        data class CheckingAccountStatus(
            override val isActive: Boolean,
            override val isCurrent: Boolean
        ) : BootstrapState

        data class CheckUpdate(
            override val isActive: Boolean,
            override val isCurrent: Boolean
        ) : BootstrapState

        fun copyCurrent(isCurrent: Boolean): BootstrapState {
            return when (this) {
                is InitializeApp -> copy(isCurrent = isCurrent)
                is CheckingLogin -> copy(isCurrent = isCurrent)
                is CheckingAccountStatus -> copy(isCurrent = isCurrent)
                is CheckUpdate -> copy(isCurrent = isCurrent)
            }
        }

        companion object {
            internal val all = listOf<BootstrapState>(
                InitializeApp(isActive = true, isCurrent = true),
                CheckUpdate(isActive = false, isCurrent = false),
                CheckingLogin(isActive = false, isCurrent = false),
                CheckingAccountStatus(isActive = false, isCurrent = false),
            )
        }
    }

    sealed interface Effect {
        data object Idle : Effect
        data object NeedsLogin : Effect
        data object NeedsUpdate : Effect
        data object NeedsAccountConfirmation : Effect
        data object NeedsTenantSelection : Effect
        data object Ok : Effect
    }
}
