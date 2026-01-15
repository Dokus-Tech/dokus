package tech.dokus.features.cashflow.presentation.peppol.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the PEPPOL Registration screen.
 *
 * Manages the PEPPOL registration lifecycle with states for:
 * - Initial setup (welcome/OGM input)
 * - Verification result (blocked or can proceed)
 * - Active/Connected status
 * - Waiting for transfer
 * - External management
 * - Error states
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface PeppolRegistrationState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - fetching current registration status.
     */
    data object Loading : PeppolRegistrationState

    /**
     * Welcome state - no registration exists, show OGM input.
     */
    data class Welcome(
        val enterpriseNumber: String = "",
        val isVerifying: Boolean = false,
        val verificationError: String? = null
    ) : PeppolRegistrationState

    /**
     * Verification result - shows whether ID is available or blocked.
     */
    data class VerificationResult(
        val result: PeppolIdVerificationResult,
        val enterpriseNumber: String,
        val isEnabling: Boolean = false
    ) : PeppolRegistrationState

    /**
     * Active state - PEPPOL is connected and working.
     */
    data class Active(
        val registration: PeppolRegistrationDto
    ) : PeppolRegistrationState

    /**
     * Waiting for transfer from another provider.
     */
    data class WaitingTransfer(
        val registration: PeppolRegistrationDto,
        val isPolling: Boolean = false
    ) : PeppolRegistrationState

    /**
     * Sending only - can send but cannot receive (blocked by another).
     */
    data class SendingOnly(
        val registration: PeppolRegistrationDto
    ) : PeppolRegistrationState

    /**
     * External - user opted to manage PEPPOL elsewhere.
     */
    data class External(
        val registration: PeppolRegistrationDto
    ) : PeppolRegistrationState

    /**
     * Pending - registration submitted, awaiting activation.
     */
    data class Pending(
        val registration: PeppolRegistrationDto
    ) : PeppolRegistrationState

    /**
     * Failed state - registration failed with error.
     */
    data class Failed(
        val registration: PeppolRegistrationDto
    ) : PeppolRegistrationState

    /**
     * Error state - failed to load initial data.
     */
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

    /** Refresh registration status */
    data object Refresh : PeppolRegistrationIntent

    /** Update enterprise number input */
    data class UpdateEnterpriseNumber(val value: String) : PeppolRegistrationIntent

    /** Verify PEPPOL ID availability */
    data object VerifyPeppolId : PeppolRegistrationIntent

    /** Enable PEPPOL (start registration) */
    data object EnablePeppol : PeppolRegistrationIntent

    /** Opt to wait for transfer */
    data object WaitForTransfer : PeppolRegistrationIntent

    /** Opt out of PEPPOL via Dokus */
    data object OptOut : PeppolRegistrationIntent

    /** Poll for transfer status */
    data object PollTransfer : PeppolRegistrationIntent

    /** Go back to welcome screen */
    data object BackToWelcome : PeppolRegistrationIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolRegistrationAction : MVIAction {

    /** Show success message */
    data class ShowSuccess(val message: String) : PeppolRegistrationAction

    /** Show error message */
    data class ShowError(val error: DokusException) : PeppolRegistrationAction

    /** Navigate back */
    data object NavigateBack : PeppolRegistrationAction
}

// ============================================================================
// HELPERS
// ============================================================================

/**
 * Maps registration status to appropriate UI state.
 */
internal fun PeppolRegistrationDto.toUiState(): PeppolRegistrationState {
    return when (status) {
        PeppolRegistrationStatus.NotConfigured -> PeppolRegistrationState.Welcome()
        PeppolRegistrationStatus.Pending -> PeppolRegistrationState.Pending(this)
        PeppolRegistrationStatus.Active -> PeppolRegistrationState.Active(this)
        PeppolRegistrationStatus.WaitingTransfer -> PeppolRegistrationState.WaitingTransfer(this)
        PeppolRegistrationStatus.SendingOnly -> PeppolRegistrationState.SendingOnly(this)
        PeppolRegistrationStatus.External -> PeppolRegistrationState.External(this)
        PeppolRegistrationStatus.Failed -> PeppolRegistrationState.Failed(this)
    }
}
