package tech.dokus.app.viewmodel

import tech.dokus.features.auth.AuthInitializer
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias BootstrapCtx = PipelineContext<BootstrapState, BootstrapIntent, BootstrapAction>

/**
 * Container for Bootstrap screen using FlowMVI.
 *
 * Manages the app initialization flow:
 * 1. Initialize server config
 * 2. Check for updates
 * 3. Check authentication status
 * 4. Check account status
 * 5. Navigate to appropriate screen
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class BootstrapContainer(
    private val authInitializer: AuthInitializer,
    private val tokenManager: TokenManager,
    private val serverConfigManager: ServerConfigManager,
) : Container<BootstrapState, BootstrapIntent, BootstrapAction> {

    private val logger = Logger.forClass<BootstrapContainer>()

    override val store: Store<BootstrapState, BootstrapIntent, BootstrapAction> =
        store(BootstrapState.Loading()) {
            reduce { intent ->
                when (intent) {
                    is BootstrapIntent.Load -> handleLoad()
                }
            }
        }

    private suspend fun BootstrapCtx.handleLoad() {
        logger.d { "Starting bootstrap process" }

        // Step 1: Initialize server config
        updateStep(BootstrapStepType.InitializeApp)
        serverConfigManager.initialize()

        // Step 2: Check for updates
        updateStep(BootstrapStepType.CheckUpdate)
        if (needsUpdate()) {
            action(BootstrapAction.NavigateToUpdate)
            return
        }

        // Step 3: Check login status
        updateStep(BootstrapStepType.CheckingLogin)
        authInitializer.initialize()
        if (!authInitializer.isAuthenticated()) {
            action(BootstrapAction.NavigateToLogin)
            return
        }

        // Step 4: Check account status
        updateStep(BootstrapStepType.CheckingAccountStatus)
        if (needsAccountConfirmation()) {
            action(BootstrapAction.NavigateToAccountConfirmation)
            return
        }

        // Step 5: Check tenant selection
        if (needsTenantSelection()) {
            action(BootstrapAction.NavigateToTenantSelection)
            return
        }

        // All checks passed, navigate to main
        logger.i { "Bootstrap complete, navigating to main" }
        action(BootstrapAction.NavigateToMain)
    }

    private suspend fun BootstrapCtx.updateStep(type: BootstrapStepType) {
        updateState {
            when (this) {
                is BootstrapState.Loading -> {
                    copy(
                        steps = steps.map { step ->
                            when {
                                step.type == type -> step.copy(isActive = true, isCurrent = true)
                                step.isCurrent -> step.copy(isCurrent = false)
                                else -> step
                            }
                        }
                    )
                }
            }
        }
    }

    private fun needsUpdate(): Boolean {
        // TODO: Implement update check
        return false
    }

    private fun needsAccountConfirmation(): Boolean {
        // TODO: Implement account status check
        // Check if user is active - fetches from network and updates local database
        // val user = userRepository.fetchCurrentUser().getOrElse {
        //     return false // TODO: Handle cases when there is no internet
        // }
        // return user.status == UserStatus.PENDING_VERIFICATION
        return false
    }

    private suspend fun needsTenantSelection(): Boolean {
        val claims = tokenManager.getCurrentClaims()
        return claims?.tenant == null
    }
}
