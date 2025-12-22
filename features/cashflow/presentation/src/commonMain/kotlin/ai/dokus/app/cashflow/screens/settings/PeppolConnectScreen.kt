package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.PeppolConnectAction
import ai.dokus.app.cashflow.viewmodel.PeppolConnectContainer
import ai.dokus.app.cashflow.viewmodel.PeppolConnectIntent
import ai.dokus.app.cashflow.viewmodel.PeppolConnectState
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldPassword
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.components.layout.TwoPaneContainer
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.PeppolProvider
import ai.dokus.foundation.domain.model.RecommandCompanySummary
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

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
                // Pop back to peppol settings
                navController.popBackStack(SettingsDestination.PeppolSettings, inclusive = false)
            }

            is PeppolConnectAction.ShowError -> {
                // Handle snackbar if needed
            }
        }
    }

    Scaffold(
        topBar = {
            PTopAppBar(title = "Connect to ${provider.displayName}")
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
                            LoadingPane("Connecting...")
                        }

                        is PeppolConnectState.Error -> {
                            ErrorPane(state as PeppolConnectState.Error)
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
                text = "Connect to ${state.provider.displayName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            // API Key field
            val apiKeyError = (state as? PeppolConnectState.EnteringCredentials)?.apiKeyError
            PTextFieldStandard(
                fieldName = "API Key",
                value = state.apiKey,
                onValueChange = { intent(PeppolConnectIntent.UpdateApiKey(it)) },
                error = apiKeyError?.let { DokusException.Validation.Generic(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // API Secret field
            val apiSecretError = (state as? PeppolConnectState.EnteringCredentials)?.apiSecretError
            PTextFieldPassword(
                fieldName = "API Secret",
                value = ai.dokus.foundation.domain.Password(state.apiSecret),
                onValueChange = { intent(PeppolConnectIntent.UpdateApiSecret(it.value)) },
                error = apiSecretError?.let { DokusException.Validation.Generic(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // Continue button
            PPrimaryButton(
                text = when {
                    isLoading -> "Connecting..."
                    else -> "Continue"
                },
                enabled = state is PeppolConnectState.EnteringCredentials,
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
            LoadingPane("Connecting...")
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
                text = "Get your credentials",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InstructionStep(1, "Visit ${provider.displayName} website")
                InstructionStep(2, "Create an account or log in")
                InstructionStep(3, "Navigate to API settings")
                InstructionStep(4, "Generate API Key and Secret")
                InstructionStep(5, "Copy credentials here")
            }

            Spacer(Modifier.height(32.dp))

            TextButton(
                onClick = { uriHandler.openUri("https://app.recommand.eu") }
            ) {
                Text(
                    text = "Open Recommand",
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
        text = "$number. $text",
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
                text = "Select your company",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Multiple companies found matching your VAT number",
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "VAT: ${company.vatNumber}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (company.enterpriseNumber.isNotBlank()) {
                Text(
                    text = "Enterprise: ${company.enterpriseNumber}",
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
                text = "No companies found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Your Recommand account has no companies matching your VAT number.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            PPrimaryButton(
                text = "Create Company on Recommand",
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
private fun IntentReceiver<PeppolConnectIntent>.ErrorPane(
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
