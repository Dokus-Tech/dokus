package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Contract for the Bootstrap screen.
 *
 * Flow:
 * 1. Load is triggered when screen appears
 * 2. Initialize app (server config)
 * 3. Check for updates
 * 4. Check login status
 * 5. Check account status
 * 6. Navigate to appropriate destination based on results
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface BootstrapState : MVIState {

    /**
     * List of bootstrap steps showing progress.
     */
    val steps: List<BootstrapStep>

    /**
     * Loading state - performing bootstrap checks.
     */
    data class Loading(
        override val steps: List<BootstrapStep> = BootstrapStep.initial,
    ) : BootstrapState
}

/**
 * Represents a single step in the bootstrap process.
 */
@Immutable
data class BootstrapStep(
    val type: BootstrapStepType,
    val isActive: Boolean,
    val isCurrent: Boolean,
) {
    companion object {
        internal val initial = listOf(
            BootstrapStep(BootstrapStepType.InitializeApp, isActive = true, isCurrent = true),
            BootstrapStep(BootstrapStepType.CheckUpdate, isActive = false, isCurrent = false),
            BootstrapStep(BootstrapStepType.CheckingLogin, isActive = false, isCurrent = false),
            BootstrapStep(BootstrapStepType.CheckingAccountStatus, isActive = false, isCurrent = false),
        )
    }
}

/**
 * Types of bootstrap steps.
 */
@Immutable
enum class BootstrapStepType {
    InitializeApp,
    CheckUpdate,
    CheckingLogin,
    CheckingAccountStatus,
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface BootstrapIntent : MVIIntent {
    /** Start the bootstrap process */
    data object Load : BootstrapIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface BootstrapAction : MVIAction {
    /** User needs to log in */
    data object NavigateToLogin : BootstrapAction

    /** App needs to be updated */
    data object NavigateToUpdate : BootstrapAction

    /** User needs to confirm their account */
    data object NavigateToAccountConfirmation : BootstrapAction

    /** User needs to select a tenant */
    data object NavigateToTenantSelection : BootstrapAction

    /** Bootstrap complete, navigate to main app */
    data object NavigateToMain : BootstrapAction
}
