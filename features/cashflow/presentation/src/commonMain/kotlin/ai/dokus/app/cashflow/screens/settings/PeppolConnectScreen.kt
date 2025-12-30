package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.PeppolConnectAction
import ai.dokus.app.cashflow.viewmodel.PeppolConnectContainer
import ai.dokus.app.cashflow.viewmodel.PeppolConnectIntent
import ai.dokus.app.cashflow.viewmodel.PeppolConnectState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.common_numbered_step
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.peppol_api_key
import tech.dokus.aura.resources.peppol_api_secret
import tech.dokus.aura.resources.peppol_connect_title_with_provider
import tech.dokus.aura.resources.peppol_create_company_on_provider
import tech.dokus.aura.resources.peppol_enterprise_value
import tech.dokus.aura.resources.peppol_get_credentials_title
import tech.dokus.aura.resources.peppol_instruction_copy_credentials
import tech.dokus.aura.resources.peppol_instruction_create_account
import tech.dokus.aura.resources.peppol_instruction_generate_keys
import tech.dokus.aura.resources.peppol_instruction_navigate_api
import tech.dokus.aura.resources.peppol_instruction_visit_site
import tech.dokus.aura.resources.peppol_multiple_companies_hint
import tech.dokus.aura.resources.peppol_no_companies_hint
import tech.dokus.aura.resources.peppol_no_companies_title
import tech.dokus.aura.resources.peppol_open_provider
import tech.dokus.aura.resources.peppol_select_company_title
import tech.dokus.aura.resources.state_connecting
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.Password
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.exceptionIfError

/**
 * Peppol provider connection screen using FlowMVI.
 * Left pane: Credentials form
 * Right pane: Instructions or company list (on large screens)
 */
@Composable
internal fun PeppolConnectScreen(
    provider: PeppolProvider,
    container: PeppolConnectContainer = container {
        parametersOf(
            PeppolConnectContainer.Companion.Params(provider)
        )
    }
) {
    val navController = LocalNavController.current
    val isLarge = LocalScreenSize.isLarge

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            PeppolConnectAction.NavigateBack -> navController.navigateUp()
            PeppolConnectAction.NavigateToSettings -> {
                // Navigate to PeppolSettings and clear the connect flow from back stack
                navController.navigate(SettingsDestination.PeppolSettings) {
                    popUpTo(SettingsDestination.PeppolSettings) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.peppol_connect_title_with_provider, provider.displayName)
            )
        }
    ) { contentPadding ->
        if (isLarge) {
            TwoPaneContainer(
                modifier = Modifier.padding(contentPadding),
                middleEffect = { EnhancedFloatingBubbles() },
                left = {
                    with(container.store) {
                        CredentialsPane(state)
                    }
                },
                right = {
                    with(container.store) {
                        RightPane(state)
                    }
                }
            )
        } else {
            // Mobile: single pane, content changes based on state
            Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
                with(container.store) {
                    when (state) {
                        is PeppolConnectState.EnteringCredentials,
                        is PeppolConnectState.LoadingCompanies -> {
                            CredentialsPane(state)
                        }

                        is PeppolConnectState.SelectingCompany -> {
                            CompanyListPane(state as PeppolConnectState.SelectingCompany)
                        }

                        is PeppolConnectState.NoCompaniesFound -> {
                            NoCompaniesPane(state)
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
                                CredentialsPane(state)
                            } else {
                                ErrorPane(errorState)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntentReceiver<PeppolConnectIntent>.CredentialsPane(
    state: PeppolConnectState
) {
    val isLoading = state is PeppolConnectState.LoadingCompanies ||
            state is PeppolConnectState.Connecting ||
            state is PeppolConnectState.CreatingCompany

    // Single error source - extracts exception from Error state
    val fieldsError = state.exceptionIfError()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Provider icon
            Icon(
                imageVector = Icons.Outlined.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.peppol_connect_title_with_provider, state.provider.displayName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(32.dp))

            // API Key field - shows error if ApiKeyRequired
            PTextFieldStandard(
                fieldName = stringResource(Res.string.peppol_api_key),
                value = state.apiKey,
                onValueChange = { intent(PeppolConnectIntent.UpdateApiKey(it)) },
                error = fieldsError.takeIf { it is DokusException.Validation.ApiKeyRequired },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // API Secret field - shows error if ApiSecretRequired or InvalidApiCredentials
            PTextFieldPassword(
                fieldName = stringResource(Res.string.peppol_api_secret),
                value = Password(state.apiSecret),
                onValueChange = { intent(PeppolConnectIntent.UpdateApiSecret(it.value)) },
                error = fieldsError.takeIf {
                    it is DokusException.Validation.ApiSecretRequired ||
                    it is DokusException.Validation.InvalidApiCredentials
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Continue button - enabled in EnteringCredentials or Error state (for retry)
            PPrimaryButton(
                text = when {
                    isLoading -> stringResource(Res.string.state_connecting)
                    else -> stringResource(Res.string.action_continue)
                },
                enabled = state is PeppolConnectState.EnteringCredentials || state is PeppolConnectState.Error,
                onClick = { intent(PeppolConnectIntent.ContinueClicked) },
                modifier = Modifier.fillMaxWidth()
            )

            if (isLoading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun IntentReceiver<PeppolConnectIntent>.RightPane(
    state: PeppolConnectState
) {
    when (state) {
        is PeppolConnectState.EnteringCredentials,
        is PeppolConnectState.LoadingCompanies -> {
            InstructionsPane(state.provider)
        }

        is PeppolConnectState.SelectingCompany -> {
            CompanyListPane(state)
        }

        is PeppolConnectState.NoCompaniesFound -> {
            NoCompaniesPane(state)
        }

        is PeppolConnectState.CreatingCompany,
        is PeppolConnectState.Connecting -> {
            LoadingPane(stringResource(Res.string.state_connecting))
        }

        is PeppolConnectState.Error -> {
            ErrorPane(state)
        }
    }
}

@Composable
private fun InstructionsPane(provider: PeppolProvider) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.peppol_get_credentials_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InstructionStep(1, stringResource(Res.string.peppol_instruction_visit_site, provider.displayName))
                InstructionStep(2, stringResource(Res.string.peppol_instruction_create_account))
                InstructionStep(3, stringResource(Res.string.peppol_instruction_navigate_api))
                InstructionStep(4, stringResource(Res.string.peppol_instruction_generate_keys))
                InstructionStep(5, stringResource(Res.string.peppol_instruction_copy_credentials))
            }

            Spacer(Modifier.height(32.dp))

            TextButton(
                onClick = { uriHandler.openUri("https://app.recommand.eu") }
            ) {
                Text(
                    text = stringResource(Res.string.peppol_open_provider, provider.displayName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Text(
        text = stringResource(Res.string.common_numbered_step, number, text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun IntentReceiver<PeppolConnectIntent>.CompanyListPane(
    state: PeppolConnectState.SelectingCompany
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.peppol_select_company_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.peppol_multiple_companies_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.companies) { company ->
                    CompanyCard(
                        company = company,
                        onClick = { intent(PeppolConnectIntent.SelectCompany(company.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyCard(
    company: RecommandCompanySummary,
    onClick: () -> Unit
) {
    DokusCardSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.common_vat_value, company.vatNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (company.enterpriseNumber.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.peppol_enterprise_value, company.enterpriseNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun IntentReceiver<PeppolConnectIntent>.NoCompaniesPane(
    state: PeppolConnectState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.peppol_no_companies_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.peppol_no_companies_hint, state.provider.displayName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            PPrimaryButton(
                text = stringResource(
                    Res.string.peppol_create_company_on_provider,
                    state.provider.displayName
                ),
                onClick = { intent(PeppolConnectIntent.CreateCompanyClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LoadingPane(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorPane(
    state: PeppolConnectState.Error
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DokusErrorContent(state.exception, state.retryHandler)
        }
    }
}
