package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.ProtocolSelector
import ai.dokus.app.auth.components.ServerConfirmationDialog
import ai.dokus.app.auth.viewmodel.ServerConnectionAction
import ai.dokus.app.auth.viewmodel.ServerConnectionContainer
import ai.dokus.app.auth.viewmodel.ServerConnectionIntent
import ai.dokus.app.auth.viewmodel.ServerConnectionState
import ai.dokus.foundation.design.components.PBackButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.config.ServerConfig
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.exceptions.DokusException
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
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

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
    container: ServerConnectionContainer = container {
        val initialConfig = if (host != null && port != null) {
            ServerConfig.fromManualEntry(host, port, protocol ?: "http")
        } else null
        parametersOf(ServerConnectionContainer.Companion.Params(initialConfig))
    }
) {
    val navController = LocalNavController.current
    val serverConfigManager: ServerConfigManager = koinInject()
    val currentServer by serverConfigManager.currentServer.collectAsState()

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ServerConnectionAction.NavigateToLogin -> navController.replace(AuthDestination.Login)
            ServerConnectionAction.NavigateBack -> navController.popBackStack()
        }
    }

    // Auto-validate if we received params from deep link
    LaunchedEffect(host, port) {
        if (host != null && port != null) {
            container.store.intent(ServerConnectionIntent.ValidateClicked)
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
                        onBackPress = { container.store.intent(ServerConnectionIntent.BackClicked) }
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
            with(container.store) {
                ServerConnectionContent(
                    state = state,
                    currentServer = currentServer
                )
            }

            // Confirmation dialog
            val previewState = state as? ServerConnectionState.Preview
            if (previewState != null) {
                ServerConfirmationDialog(
                    config = previewState.config,
                    serverInfo = previewState.serverInfo,
                    onConfirm = { container.store.intent(ServerConnectionIntent.ConfirmConnection) },
                    onDismiss = { container.store.intent(ServerConnectionIntent.CancelPreview) }
                )
            }
        }
    }
}

@Composable
private fun IntentReceiver<ServerConnectionIntent>.ServerConnectionContent(
    state: ServerConnectionState,
    currentServer: ServerConfig
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
                        onProtocolSelected = { intent(ServerConnectionIntent.UpdateProtocol(it)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Host input
                    val hostError = (state as? ServerConnectionState.Input)?.hostError
                    PTextFieldStandard(
                        fieldName = "Host or IP Address",
                        value = state.host,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        error = hostError?.let { DokusException.Validation.Generic(it) },
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { intent(ServerConnectionIntent.UpdateHost(it)) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Port input
                    val portError = (state as? ServerConnectionState.Input)?.portError
                    PTextFieldStandard(
                        fieldName = "Port",
                        value = state.port,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        error = portError?.let { DokusException.Validation.Generic(it) },
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { intent(ServerConnectionIntent.UpdatePort(it)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Validate button
                    PPrimaryButton(
                        text = when (state) {
                            is ServerConnectionState.Validating -> "Validating..."
                            is ServerConnectionState.Connecting -> "Connecting..."
                            else -> "Validate Connection"
                        },
                        enabled = state !is ServerConnectionState.Validating &&
                                state !is ServerConnectionState.Connecting,
                        onClick = { intent(ServerConnectionIntent.ValidateClicked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error display
            val errorState = state as? ServerConnectionState.Error
            if (errorState != null) {
                Spacer(modifier = Modifier.height(16.dp))
                DokusErrorContent(
                    exception = errorState.exception,
                    retryHandler = RetryHandler { intent(ServerConnectionIntent.ValidateClicked) },
                    compact = true
                )
            }

            // Loading indicator
            if (state is ServerConnectionState.Validating || state is ServerConnectionState.Connecting) {
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
                        text = if (state is ServerConnectionState.Validating) {
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
                onClick = { intent(ServerConnectionIntent.ResetToCloud) },
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
