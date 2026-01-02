package tech.dokus.features.cashflow.presentation.settings.screen

import tech.dokus.features.cashflow.mvi.PeppolConnectIntent
import tech.dokus.features.cashflow.mvi.PeppolConnectState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_connect_title_with_provider
import tech.dokus.aura.resources.state_connecting
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.features.cashflow.presentation.settings.components.CompanyListPane
import tech.dokus.features.cashflow.presentation.settings.components.CredentialsPane
import tech.dokus.features.cashflow.presentation.settings.components.ErrorPane
import tech.dokus.features.cashflow.presentation.settings.components.LoadingPane
import tech.dokus.features.cashflow.presentation.settings.components.NoCompaniesPane
import tech.dokus.features.cashflow.presentation.settings.components.RightPane
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

/**
 * Peppol provider connection screen using FlowMVI.
 * Left pane: Credentials form
 * Right pane: Instructions or company list (on large screens)
 */
@Composable
internal fun PeppolConnectScreen(
    provider: PeppolProvider,
    state: PeppolConnectState,
    onIntent: (PeppolConnectIntent) -> Unit
) {
    val isLarge = LocalScreenSize.isLarge

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.peppol_connect_title_with_provider, provider.localized)
            )
        }
    ) { contentPadding ->
        if (isLarge) {
            TwoPaneContainer(
                modifier = Modifier.padding(contentPadding),
                middleEffect = { EnhancedFloatingBubbles() },
                left = {
                    CredentialsPane(state, onIntent)
                },
                right = {
                    RightPane(state, onIntent)
                }
            )
        } else {
            // Mobile: single pane, content changes based on state
            Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                when (state) {
                    is PeppolConnectState.EnteringCredentials,
                    is PeppolConnectState.LoadingCompanies -> {
                        CredentialsPane(state, onIntent)
                    }

                    is PeppolConnectState.SelectingCompany -> {
                        CompanyListPane(state as PeppolConnectState.SelectingCompany, onIntent)
                    }

                    is PeppolConnectState.NoCompaniesFound -> {
                        NoCompaniesPane(state, onIntent)
                    }

                    is PeppolConnectState.CreatingCompany,
                    is PeppolConnectState.Connecting -> {
                        LoadingPane(stringResource(Res.string.state_connecting))
                    }

                    is PeppolConnectState.Error -> {
                        // For field-level errors, show credentials pane so user can edit
                        val errorState = state as PeppolConnectState.Error
                        val exception = errorState.exception
                        if (exception is DokusException.Validation.ApiKeyRequired ||
                            exception is DokusException.Validation.ApiSecretRequired ||
                            exception is DokusException.Validation.InvalidApiCredentials) {
                            CredentialsPane(state, onIntent)
                        } else {
                            ErrorPane(errorState)
                        }
                    }
                }
            }
        }
    }
}
