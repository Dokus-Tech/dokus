package tech.dokus.features.cashflow.presentation.peppol.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
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
    /** UX hint: surface that Peppol is a premium feature. */
    val showPremiumHint: Boolean = true,
)

@Immutable
sealed interface PeppolRegistrationState : MVIState, DokusState<Nothing> {

    data object Loading : PeppolRegistrationState

    data class Fresh(
        val context: PeppolSetupContext,
        val isEnabling: Boolean = false,
    ) : PeppolRegistrationState

    data class Activating(
        val context: PeppolSetupContext,
    ) : PeppolRegistrationState

    data class Active(
        val context: PeppolSetupContext,
    ) : PeppolRegistrationState

    data class Blocked(
        val context: PeppolSetupContext,
        val isWorking: Boolean = false,
    ) : PeppolRegistrationState

    data class WaitingTransfer(
        val context: PeppolSetupContext,
    ) : PeppolRegistrationState

    data class SendingOnly(
        val context: PeppolSetupContext,
    ) : PeppolRegistrationState

    data class External(
        val context: PeppolSetupContext,
    ) : PeppolRegistrationState

    data class Failed(
        val context: PeppolSetupContext,
        val message: String? = null,
        val isRetrying: Boolean = false,
    ) : PeppolRegistrationState

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler
    ) : PeppolRegistrationState, DokusState.Error<Nothing>
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
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolRegistrationAction : MVIAction {
    data class ShowError(val error: DokusException) : PeppolRegistrationAction
    data object NavigateToHome : PeppolRegistrationAction
}

