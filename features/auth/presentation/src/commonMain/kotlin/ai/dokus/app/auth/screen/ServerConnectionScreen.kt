package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.ProtocolSelector
import ai.dokus.app.auth.components.ServerConfirmationDialog
import ai.dokus.app.auth.viewmodel.ServerConnectionViewModel
import ai.dokus.foundation.design.components.PBackButton
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.config.ServerConfig
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.replace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen for connecting to a self-hosted Dokus server.
 *
 * Features:
 * - Protocol selection (HTTP/HTTPS)
 * - Host/IP input
 * - Port input
 * - Server validation
 * - Confirmation dialog
 * - Reset to cloud option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerConnectionScreen(
    host: String? = null,
    port: Int? = null,
    protocol: String? = null,
    viewModel: ServerConnectionViewModel = koinViewModel {
        val initialConfig = if (host != null && port != null) {
            ServerConfig.fromManualEntry(host, port, protocol ?: "http")
        } else null
        parametersOf(initialConfig)
    }
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val currentServer by viewModel.currentServer.collectAsState()

    // Handle navigation effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ServerConnectionViewModel.Effect.NavigateToLogin -> {
                    navController.replace(AuthDestination.Login)
                }
                is ServerConnectionViewModel.Effect.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    // Auto-validate if we received params from deep link
    LaunchedEffect(host, port) {
        if (host != null && port != null) {
            viewModel.validateServer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Connect to Server",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    PBackButton(
                        modifier = Modifier.padding(start = 8.dp),
                        onBackPress = { viewModel.navigateBack() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .withContentPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.limitWidthCenteredContent()
                ) {
                    // Current server info (if not cloud)
                    if (!currentServer.isCloud) {
                        CurrentServerCard(currentServer)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Server details card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Server Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Protocol selector
                            Text(
                                text = "Protocol",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ProtocolSelector(
                                selectedProtocol = state.protocol,
                                onProtocolSelected = { viewModel.onProtocolChange(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Host input
                            PTextFieldStandard(
                                fieldName = "Host or IP Address",
                                value = state.host,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next
                                ),
                                error = state.hostError?.let { DokusException.Validation.Generic(it) },
                                modifier = Modifier.fillMaxWidth(),
                                onValueChange = { viewModel.onHostChange(it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Port input
                            PTextFieldStandard(
                                fieldName = "Port",
                                value = state.port,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                error = state.portError?.let { DokusException.Validation.Generic(it) },
                                modifier = Modifier.fillMaxWidth(),
                                onValueChange = { viewModel.onPortChange(it) }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Validate button
                            PPrimaryButton(
                                text = when (screenState) {
                                    is ServerConnectionViewModel.ScreenState.Validating -> "Validating..."
                                    is ServerConnectionViewModel.ScreenState.Connecting -> "Connecting..."
                                    else -> "Validate Connection"
                                },
                                enabled = screenState !is ServerConnectionViewModel.ScreenState.Validating &&
                                        screenState !is ServerConnectionViewModel.ScreenState.Connecting,
                                onClick = { viewModel.validateServer() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Error display
                    val errorState = screenState as? ServerConnectionViewModel.ScreenState.Error
                    if (errorState != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        DokusErrorContent(
                            exception = errorState.error.asDokusException,
                            retryHandler = RetryHandler { viewModel.retry() },
                            compact = true
                        )
                    }

                    // Loading indicator
                    if (screenState is ServerConnectionViewModel.ScreenState.Validating ||
                        screenState is ServerConnectionViewModel.ScreenState.Connecting) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (screenState is ServerConnectionViewModel.ScreenState.Validating) {
                                    "Checking server..."
                                } else {
                                    "Connecting..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Help section
                    HelpCard()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reset to cloud option
                    TextButton(
                        onClick = { viewModel.resetToCloud() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Use Dokus Cloud instead")
                    }
                }
            }

            // Confirmation dialog
            val previewState = screenState as? ServerConnectionViewModel.ScreenState.Preview
            if (previewState != null) {
                ServerConfirmationDialog(
                    config = previewState.config,
                    serverInfo = previewState.serverInfo,
                    onConfirm = { viewModel.confirmConnection() },
                    onDismiss = { viewModel.cancelPreview() }
                )
            }
        }
    }
}

@Composable
private fun CurrentServerCard(currentServer: ServerConfig) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Currently connected to:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentServer.name ?: currentServer.host,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HelpCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Need help?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ask your system administrator for the server address, port, and protocol. " +
                            "You can also scan a QR code from the server admin panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
