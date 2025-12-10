package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.ConnectionTestState
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsViewModel
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.peppol_api_key
import ai.dokus.app.resources.generated.peppol_api_secret
import ai.dokus.app.resources.generated.peppol_company_id
import ai.dokus.app.resources.generated.peppol_configuration
import ai.dokus.app.resources.generated.peppol_connected
import ai.dokus.app.resources.generated.peppol_connection_status
import ai.dokus.app.resources.generated.peppol_credentials
import ai.dokus.app.resources.generated.peppol_delete_settings
import ai.dokus.app.resources.generated.peppol_enabled
import ai.dokus.app.resources.generated.peppol_not_configured
import ai.dokus.app.resources.generated.peppol_participant_id
import ai.dokus.app.resources.generated.peppol_settings_title
import ai.dokus.app.resources.generated.peppol_test_connection
import ai.dokus.app.resources.generated.peppol_test_mode
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.app.resources.generated.save_changes
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Peppol E-Invoicing settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun PeppolSettingsScreen(
    viewModel: PeppolSettingsViewModel = koinViewModel()
) {
    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.peppol_settings_title)
            )
        }
    ) { contentPadding ->
        PeppolSettingsContent(
            viewModel = viewModel,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Peppol settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun PeppolSettingsContent(
    viewModel: PeppolSettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val connectionTestState by viewModel.connectionTestState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadSettings()
    }

    when {
        state.isLoading() -> {
            Box(
                modifier = modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .withContentPaddingForScrollable(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connection Status Card
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.peppol_connection_status),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(12.dp))

                        val isConfigured = state.isSuccess() && state.let {
                            (it as? ai.dokus.app.core.state.DokusState.Success)?.data != null
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isConfigured) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isConfigured) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isConfigured) Res.string.peppol_connected
                                    else Res.string.peppol_not_configured
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isConfigured) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Credentials Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.peppol_credentials),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.peppol_company_id),
                            value = formState.companyId,
                            onValueChange = { viewModel.updateCompanyId(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        formState.errors["companyId"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.peppol_participant_id),
                            value = formState.peppolId,
                            onValueChange = { viewModel.updatePeppolId(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        formState.errors["peppolId"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.peppol_api_key),
                            value = formState.apiKey,
                            onValueChange = { viewModel.updateApiKey(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (formState.isEditing) {
                            Text(
                                text = "Leave blank to keep existing key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        formState.errors["apiKey"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.peppol_api_secret),
                            value = formState.apiSecret,
                            onValueChange = { viewModel.updateApiSecret(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (formState.isEditing) {
                            Text(
                                text = "Leave blank to keep existing secret",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        formState.errors["apiSecret"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Configuration Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.peppol_configuration),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.peppol_enabled),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = formState.isEnabled,
                                onCheckedChange = { viewModel.updateIsEnabled(it) }
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.peppol_test_mode),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Switch(
                                checked = formState.testMode,
                                onCheckedChange = { viewModel.updateTestMode(it) }
                            )
                        }
                    }
                }

                // Test Connection & Save Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    POutlinedButton(
                        text = stringResource(Res.string.peppol_test_connection),
                        enabled = connectionTestState !is ConnectionTestState.Testing,
                        onClick = { viewModel.testConnection() },
                        modifier = Modifier.weight(1f)
                    )

                    PPrimaryButton(
                        text = stringResource(Res.string.save_changes),
                        enabled = !state.isLoading(),
                        onClick = { viewModel.saveSettings() },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Connection Test Result
                when (connectionTestState) {
                    is ConnectionTestState.Testing -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Testing connection...",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    is ConnectionTestState.Success -> {
                        Text(
                            text = "Connection successful!",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    is ConnectionTestState.Failed -> {
                        Text(
                            text = (connectionTestState as ConnectionTestState.Failed).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    else -> {}
                }

                // Danger Zone - Delete Settings
                if (formState.isEditing) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.profile_danger_zone),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Deleting Peppol settings will disable e-invoicing capabilities.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            POutlinedButton(
                                text = stringResource(Res.string.peppol_delete_settings),
                                onClick = { viewModel.deleteSettings() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
