package tech.dokus.features.cashflow.presentation.peppol.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Peppol registration/settings flow.
 *
 * UX goals:
 * - No VAT input (workspace already has VAT).
 * - Minimal user effort; show the right next step automatically.
 * - Provider names are never shown to users.
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class PeppolSetupContext(
    val companyName: String,
    /** Participant ID in `0208:BE...` format. */
    val peppolId: String,
)

enum class PeppolRegistrationPhase {
    Fresh,
    Activating,
    Active,
    Blocked,
    WaitingTransfer,
    SendingOnly,
    External,
    Failed,
}

@Immutable
data class PeppolRegistrationState(
    val setupContext: DokusState<PeppolSetupContext> = DokusState.loading(),
    val phase: PeppolRegistrationPhase = PeppolRegistrationPhase.Fresh,
    val isWorking: Boolean = false,
    val isRetrying: Boolean = false,
    val failureMessage: String? = null,
    val actionError: DokusException? = null,
) : MVIState {
    companion object {
        val initial by lazy { PeppolRegistrationState() }
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface PeppolRegistrationIntent : MVIIntent {
    data object Refresh : PeppolRegistrationIntent

    data object EnablePeppol : PeppolRegistrationIntent
    data object EnableSendingOnly : PeppolRegistrationIntent
    data object WaitForTransfer : PeppolRegistrationIntent

    data object PollTransfer : PeppolRegistrationIntent

    data object NotNow : PeppolRegistrationIntent
    data object Continue : PeppolRegistrationIntent

    data object Retry : PeppolRegistrationIntent

    data object DismissActionError : PeppolRegistrationIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolRegistrationAction : MVIAction {
    data object NavigateToHome : PeppolRegistrationAction
}
