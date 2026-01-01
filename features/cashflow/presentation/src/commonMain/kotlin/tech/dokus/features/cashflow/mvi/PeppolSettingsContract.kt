package tech.dokus.features.cashflow.mvi

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.RecommandCompanySummary
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Peppol settings management screen.
 *
 * Flow:
 * 1. Loading → Initial load of settings
 * 2. NotConfigured → No Peppol connection, show provider selection
 * 3. Connected → Peppol is configured, show status and disconnect option
 * 4. Deleting → User is disconnecting from Peppol
 * 5. Error → Error occurred with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface PeppolSettingsState : MVIState, DokusState<PeppolSettingsDto?> {

    /**
     * Initial loading state - fetching settings from server.
     */
    data object Loading : PeppolSettingsState, DokusState.Loading<PeppolSettingsDto?>

    /**
     * No Peppol connection configured - show provider selection.
     */
    data object NotConfigured : PeppolSettingsState

    /**
     * Connected to Peppol - show status and management options.
     */
    data class Connected(
        val settings: PeppolSettingsDto,
        val connectedCompany: RecommandCompanySummary? = null,
    ) : PeppolSettingsState

    /**
     * Deleting/disconnecting from Peppol.
     */
    data class Deleting(
        val settings: PeppolSettingsDto,
    ) : PeppolSettingsState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : PeppolSettingsState, DokusState.Error<PeppolSettingsDto?>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface PeppolSettingsIntent : MVIIntent {
    /** Load Peppol settings on screen init */
    data object LoadSettings : PeppolSettingsIntent

    /** User selected a provider to connect */
    data class SelectProvider(val provider: PeppolProvider) : PeppolSettingsIntent

    /** User clicked delete/disconnect button */
    data object DeleteSettingsClicked : PeppolSettingsIntent

    /** User confirmed deletion in dialog */
    data object ConfirmDelete : PeppolSettingsIntent

    /** User cancelled deletion in dialog */
    data object CancelDelete : PeppolSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolSettingsAction : MVIAction {
    /** Navigate to Peppol connect screen for the selected provider */
    data class NavigateToPeppolConnect(val provider: PeppolProvider) : PeppolSettingsAction

    /** Navigate back to previous screen */
    data object NavigateBack : PeppolSettingsAction

    /** Show deletion confirmation dialog */
    data object ShowDeleteConfirmation : PeppolSettingsAction

    /** Show success message after deletion */
    data object ShowDeleteSuccess : PeppolSettingsAction
}
