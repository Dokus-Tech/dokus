package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.PeppolConnectionState
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsViewModel
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.peppol_connected
import ai.dokus.app.resources.generated.peppol_connection_status
import ai.dokus.app.resources.generated.peppol_delete_settings
import ai.dokus.app.resources.generated.peppol_not_configured
import ai.dokus.app.resources.generated.peppol_settings_title
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.destinations.SettingsDestination
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
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
import tech.dokus.foundation.app.state.isLoading

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
 * Simplified version - shows connection status and connect/disconnect buttons.
 * The actual credentials entry is handled in PeppolConnectScreen.
 */
@Composable
fun PeppolSettingsContent(
    viewModel: PeppolSettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val connectedCompany by viewModel.connectedCompany.collectAsState()

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

                // Connect Provider Button - Only show if not connected
                if (!isConnected) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Connect to Peppol",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Connect your Peppol Access Point provider to enable e-invoicing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(16.dp))

                            PPrimaryButton(
                                text = "Connect Provider",
                                onClick = { navController.navigate(SettingsDestination.PeppolProviders) },
                                modifier = Modifier.fillMaxWidth()
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

