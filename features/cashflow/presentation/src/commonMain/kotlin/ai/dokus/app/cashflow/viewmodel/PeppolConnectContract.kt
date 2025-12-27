package ai.dokus.app.cashflow.viewmodel

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.domain.model.RecommandCompanySummary
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Peppol provider connection screen.
 *
 * Flow:
 * 1. EnteringCredentials → User enters API Key and Secret
 * 2. LoadingCompanies → Fetching companies from Recommand
 * 3. SelectingCompany → User picks from company list
 *    OR NoCompaniesFound → Prompt to create company
 * 4. Connecting → Saving settings
 * 5. Connected → Success, navigate back
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface PeppolConnectState : MVIState, DokusState<Nothing> {
    val provider: PeppolProvider
    val apiKey: String
    val apiSecret: String

    /**
     * Initial state - user enters credentials.
     */
    data class EnteringCredentials(
        override val provider: PeppolProvider,
        override val apiKey: String = "",
        override val apiSecret: String = "",
    ) : PeppolConnectState

    /**
     * Loading companies from Recommand API.
     */
    data class LoadingCompanies(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
    ) : PeppolConnectState

    /**
     * User selects from available companies.
     */
    data class SelectingCompany(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
        val companies: List<RecommandCompanySummary>,
    ) : PeppolConnectState

    /**
     * No companies found - prompt user to create one.
     */
    data class NoCompaniesFound(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
    ) : PeppolConnectState

    /**
     * Creating company on Recommand.
     */
    data class CreatingCompany(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
    ) : PeppolConnectState

    /**
     * Connecting to selected company.
     */
    data class Connecting(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
        val selectedCompanyId: String,
    ) : PeppolConnectState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val provider: PeppolProvider,
        override val apiKey: String,
        override val apiSecret: String,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : PeppolConnectState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface PeppolConnectIntent : MVIIntent {
    /** User typed in API key field */
    data class UpdateApiKey(val value: String) : PeppolConnectIntent

    /** User typed in API secret field */
    data class UpdateApiSecret(val value: String) : PeppolConnectIntent

    /** User clicked Continue button */
    data object ContinueClicked : PeppolConnectIntent

    /** User selected a company from the list */
    data class SelectCompany(val companyId: String) : PeppolConnectIntent

    /** User clicked to create company on Recommand */
    data object CreateCompanyClicked : PeppolConnectIntent

    /** User clicked back button */
    data object BackClicked : PeppolConnectIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolConnectAction : MVIAction {
    /** Navigate back to previous screen */
    data object NavigateBack : PeppolConnectAction

    /** Navigate to settings after successful connection */
    data object NavigateToSettings : PeppolConnectAction
}
