package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.PeppolConnectionState
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsViewModel
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.peppol_api_key
import ai.dokus.app.resources.generated.peppol_api_secret
import ai.dokus.app.resources.generated.peppol_configuration
import ai.dokus.app.resources.generated.peppol_connected
import ai.dokus.app.resources.generated.peppol_connection_status
import ai.dokus.app.resources.generated.peppol_credentials
import ai.dokus.app.resources.generated.peppol_delete_settings
import ai.dokus.app.resources.generated.peppol_enabled
import ai.dokus.app.resources.generated.peppol_not_configured
import ai.dokus.app.resources.generated.peppol_settings_title
import ai.dokus.app.resources.generated.peppol_test_mode
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.domain.model.RecommandCompanySummary
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess

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
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedCompany by viewModel.connectedCompany.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadSettings()
    }

    // Handle connection dialogs
    when (val connState = connectionState) {
        is PeppolConnectionState.SelectCompany -> {
            CompanySelectionDialog(
                candidates = connState.candidates,
                onSelect = { viewModel.selectCompany(it.id) },
                onDismiss = { viewModel.cancelConnection() }
            )
        }
        is PeppolConnectionState.ConfirmCreateCompany -> {
            CreateCompanyConfirmationDialog(
                onConfirm = { viewModel.confirmCreateCompany() },
                onDismiss = { viewModel.cancelConnection() }
            )
        }
        is PeppolConnectionState.Error -> {
            ErrorDialog(
                message = connState.message,
                onDismiss = { viewModel.resetConnectionState() }
            )
        }
        else -> {}
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
            val isConnected = connectionState is PeppolConnectionState.Connected

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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isConnected) Res.string.peppol_connected
                                    else Res.string.peppol_not_configured
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isConnected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                            )
                        }

                        // Show connected company info
                        connectedCompany?.let { company ->
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Connected to:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = company.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "VAT: ${company.vatNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Credentials Section - Only show if not connected
                if (!isConnected) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.peppol_credentials),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Enter your Recommand API credentials to connect to Peppol. Your company will be automatically matched by VAT number.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(16.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.peppol_api_key),
                                value = formState.apiKey,
                                onValueChange = { viewModel.updateApiKey(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
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
                            formState.errors["apiSecret"]?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
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

                // Connect Button - Only show if not connected
                if (!isConnected) {
                    val isConnecting = connectionState is PeppolConnectionState.Connecting

                    PPrimaryButton(
                        text = if (isConnecting) "Connecting..." else "Connect",
                        enabled = !isConnecting && !state.isLoading(),
                        onClick = { viewModel.connect() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isConnecting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Connecting to Peppol...",
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Danger Zone - Delete Settings (only when connected)
                if (isConnected) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.profile_danger_zone),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Disconnecting Peppol will disable e-invoicing capabilities.",
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

/**
 * Dialog for selecting a company when multiple matches are found.
 */
@Composable
private fun CompanySelectionDialog(
    candidates: List<RecommandCompanySummary>,
    onSelect: (RecommandCompanySummary) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCompany by remember { mutableStateOf<RecommandCompanySummary?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Company") },
        text = {
            Column {
                Text(
                    text = "Multiple companies found matching your VAT number. Please select one:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                candidates.forEach { company ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCompany = company }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCompany == company,
                            onClick = { selectedCompany = company }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = company.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "VAT: ${company.vatNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCompany?.let { onSelect(it) } },
                enabled = selectedCompany != null
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for confirming company creation on Recommand.
 */
@Composable
private fun CreateCompanyConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Company") },
        text = {
            Column {
                Text(
                    text = "No company found on Recommand matching your VAT number.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Would you like Dokus to create one using your company information?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "The company will be created with your legal name, VAT number, and address from your workspace settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Create & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for showing error messages.
 */
@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connection Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
