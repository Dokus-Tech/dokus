package tech.dokus.features.cashflow.presentation.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
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
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.features.cashflow.mvi.PeppolConnectIntent
import tech.dokus.features.cashflow.mvi.PeppolConnectState
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.extensions.websiteUrl

@Composable
internal fun CredentialsPane(
    state: PeppolConnectState,
    onIntent: (PeppolConnectIntent) -> Unit
) {
    val isLoading = state is PeppolConnectState.LoadingCompanies ||
            state is PeppolConnectState.Connecting ||
            state is PeppolConnectState.CreatingCompany

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

            PTextFieldStandard(
                fieldName = stringResource(Res.string.peppol_api_key),
                value = state.apiKey,
                onValueChange = { onIntent(PeppolConnectIntent.UpdateApiKey(it)) },
                error = fieldsError.takeIf { it is DokusException.Validation.ApiKeyRequired },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            PTextFieldPassword(
                fieldName = stringResource(Res.string.peppol_api_secret),
                value = Password(state.apiSecret),
                onValueChange = { onIntent(PeppolConnectIntent.UpdateApiSecret(it.value)) },
                error = fieldsError.takeIf {
                    it is DokusException.Validation.ApiSecretRequired ||
                    it is DokusException.Validation.InvalidApiCredentials
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            PPrimaryButton(
                text = when {
                    isLoading -> stringResource(Res.string.state_connecting)
                    else -> stringResource(Res.string.action_continue)
                },
                enabled = state is PeppolConnectState.EnteringCredentials || state is PeppolConnectState.Error,
                onClick = { onIntent(PeppolConnectIntent.ContinueClicked) },
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
internal fun RightPane(
    state: PeppolConnectState,
    onIntent: (PeppolConnectIntent) -> Unit
) {
    when (state) {
        is PeppolConnectState.EnteringCredentials,
        is PeppolConnectState.LoadingCompanies -> {
            InstructionsPane(state.provider)
        }

        is PeppolConnectState.SelectingCompany -> {
            CompanyListPane(state, onIntent)
        }

        is PeppolConnectState.NoCompaniesFound -> {
            NoCompaniesPane(state, onIntent)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.peppol_get_credentials_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InstructionStep(1, stringResource(Res.string.peppol_instruction_visit_site, provider.displayName))
            InstructionStep(2, stringResource(Res.string.peppol_instruction_create_account))
            InstructionStep(3, stringResource(Res.string.peppol_instruction_navigate_api))
            InstructionStep(4, stringResource(Res.string.peppol_instruction_generate_keys))
            InstructionStep(5, stringResource(Res.string.peppol_instruction_copy_credentials))
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.peppol_open_provider, provider.displayName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = { uriHandler.openUri(provider.websiteUrl) }
            )
        )
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
internal fun CompanyListPane(
    state: PeppolConnectState.SelectingCompany,
    onIntent: (PeppolConnectIntent) -> Unit
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
                        onClick = { onIntent(PeppolConnectIntent.SelectCompany(company.id)) }
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
internal fun NoCompaniesPane(
    state: PeppolConnectState,
    onIntent: (PeppolConnectIntent) -> Unit
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
                onClick = { onIntent(PeppolConnectIntent.CreateCompanyClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun LoadingPane(message: String) {
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
internal fun ErrorPane(
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
