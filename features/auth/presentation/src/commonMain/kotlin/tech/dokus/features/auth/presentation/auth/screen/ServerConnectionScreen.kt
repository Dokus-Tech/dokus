package tech.dokus.features.auth.presentation.auth.screen

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
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_checking_server
import tech.dokus.aura.resources.auth_connecting
import tech.dokus.aura.resources.auth_currently_connected_to
import tech.dokus.aura.resources.auth_help_description
import tech.dokus.aura.resources.auth_host_label
import tech.dokus.aura.resources.auth_need_help
import tech.dokus.aura.resources.auth_port_label
import tech.dokus.aura.resources.auth_protocol_label
import tech.dokus.aura.resources.auth_server_details_title
import tech.dokus.aura.resources.auth_use_cloud
import tech.dokus.aura.resources.auth_validate_connection
import tech.dokus.aura.resources.auth_validating
import tech.dokus.aura.resources.connect_to_server
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.config.ServerConfig
import tech.dokus.features.auth.mvi.ServerConnectionIntent
import tech.dokus.features.auth.mvi.ServerConnectionState
import tech.dokus.features.auth.presentation.auth.components.ProtocolSelector
import tech.dokus.features.auth.presentation.auth.components.ServerConfirmationDialog
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing

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
    state: ServerConnectionState,
    currentServer: ServerConfig?,
    onIntent: (ServerConnectionIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.connect_to_server),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    PBackButton(
                        onBackPress = { onIntent(ServerConnectionIntent.BackClicked) }
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
            ServerConnectionContent(
                state = state,
                currentServer = currentServer,
                onIntent = onIntent
            )

            // Confirmation dialog
            val previewState = state as? ServerConnectionState.Preview
            if (previewState != null) {
                ServerConfirmationDialog(
                    config = previewState.config,
                    serverInfo = previewState.serverInfo,
                    onConfirm = { onIntent(ServerConnectionIntent.ConfirmConnection) },
                    onDismiss = { onIntent(ServerConnectionIntent.CancelPreview) }
                )
            }
        }
    }
}

@Composable
private fun ServerConnectionContent(
    state: ServerConnectionState,
    currentServer: ServerConfig?,
    onIntent: (ServerConnectionIntent) -> Unit,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
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
            currentServer?.takeIf { !it.isCloud }?.let { server ->
                CurrentServerCard(server)
                Spacer(modifier = Modifier.height(spacing.xLarge))
            }

            // Server details card
            DokusCard(
                modifier = Modifier.fillMaxWidth(),
                padding = DokusCardPadding.Default,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.auth_server_details_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    // Protocol selector
                    Text(
                        text = stringResource(Res.string.auth_protocol_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    ProtocolSelector(
                        selectedProtocol = state.protocol,
                        onProtocolSelected = { onIntent(ServerConnectionIntent.UpdateProtocol(it)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    // Host input
                    val hostError = (state as? ServerConnectionState.Input)?.hostError
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.auth_host_label),
                        value = state.host,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        error = hostError,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { onIntent(ServerConnectionIntent.UpdateHost(it)) }
                    )

                    Spacer(modifier = Modifier.height(spacing.large))

                    // Port input
                    val portError = (state as? ServerConnectionState.Input)?.portError
                    PTextFieldStandard(
                        fieldName = stringResource(Res.string.auth_port_label),
                        value = state.port,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        error = portError,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = { onIntent(ServerConnectionIntent.UpdatePort(it)) }
                    )

                    Spacer(modifier = Modifier.height(spacing.xLarge))

                    // Validate button
                    PPrimaryButton(
                        text = when (state) {
                            is ServerConnectionState.Validating -> stringResource(Res.string.auth_validating)
                            is ServerConnectionState.Connecting -> stringResource(Res.string.auth_connecting)
                            else -> stringResource(Res.string.auth_validate_connection)
                        },
                        enabled = state !is ServerConnectionState.Validating &&
                            state !is ServerConnectionState.Connecting,
                        onClick = { onIntent(ServerConnectionIntent.ValidateClicked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error display
            val errorState = state as? ServerConnectionState.Error
            if (errorState != null) {
                Spacer(modifier = Modifier.height(spacing.large))
                DokusErrorContent(
                    exception = errorState.exception,
                    retryHandler = RetryHandler { onIntent(ServerConnectionIntent.ValidateClicked) },
                    compact = true
                )
            }

            // Loading indicator
            if (state is ServerConnectionState.Validating || state is ServerConnectionState.Connecting) {
                Spacer(modifier = Modifier.height(spacing.xLarge))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                    Spacer(modifier = Modifier.width(spacing.medium))
                    Text(
                        text = if (state is ServerConnectionState.Validating) {
                            stringResource(Res.string.auth_checking_server)
                        } else {
                            stringResource(Res.string.auth_connecting)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xLarge))

            // Help section
            HelpCard()

            Spacer(modifier = Modifier.height(spacing.xLarge))

            // Reset to cloud option
            TextButton(
                onClick = { onIntent(ServerConnectionIntent.ResetToCloud) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(sizing.iconSmall)
                )
                Spacer(modifier = Modifier.width(spacing.small))
                Text(text = stringResource(Res.string.auth_use_cloud))
            }
        }
    }
}

@Composable
private fun CurrentServerCard(currentServer: ServerConfig) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier.padding(spacing.large),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(sizing.iconSmallMedium)
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column {
                Text(
                    text = stringResource(Res.string.auth_currently_connected_to),
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
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    DokusGlassSurface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(spacing.large),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(sizing.iconSmallMedium)
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            Column {
                Text(
                    text = stringResource(Res.string.auth_need_help),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(spacing.xSmall))
                Text(
                    text = stringResource(Res.string.auth_help_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ServerConnectionScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ServerConnectionScreen(
            state = ServerConnectionState.Input(),
            currentServer = ServerConfig.Cloud,
            onIntent = {},
        )
    }
}
