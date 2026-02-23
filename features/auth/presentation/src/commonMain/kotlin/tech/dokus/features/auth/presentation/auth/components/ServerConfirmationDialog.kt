package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_connect
import tech.dokus.aura.resources.auth_server_connecting_will
import tech.dokus.aura.resources.auth_server_features_label
import tech.dokus.aura.resources.auth_server_found
import tech.dokus.aura.resources.auth_server_logout_warning
import tech.dokus.aura.resources.auth_server_name_label
import tech.dokus.aura.resources.auth_server_reauth_warning
import tech.dokus.aura.resources.auth_server_reset_warning
import tech.dokus.aura.resources.auth_server_url
import tech.dokus.aura.resources.auth_server_version
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Confirmation dialog shown after server validation.
 *
 * Displays server information and warns the user that connecting will:
 * - Clear all local data and tokens
 * - Log them out of current server
 * - Require re-authentication on new server
 *
 * @param config The validated server configuration
 * @param serverInfo Server information returned from validation
 * @param onConfirm Called when user confirms connection
 * @param onDismiss Called when dialog is dismissed
 */
@androidx.compose.runtime.Composable
fun ServerConfirmationDialog(
    config: ServerConfig,
    serverInfo: ServerInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DokusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.auth_server_found),
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)
            ) {
                // Server info card
                DokusCardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = DokusCardVariant.Soft,
                ) {
                    Column(modifier = Modifier.padding(Constraints.Spacing.medium)) {
                        ServerInfoRow(stringResource(Res.string.auth_server_name_label), serverInfo.name)
                        Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                        ServerInfoRow(stringResource(Res.string.auth_server_version), serverInfo.version)
                        Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                        ServerInfoRow(stringResource(Res.string.auth_server_url), config.baseUrl)
                        if (serverInfo.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                            ServerInfoRow(
                                stringResource(Res.string.auth_server_features_label),
                                serverInfo.features.joinToString(", ")
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Warning section
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Constraints.Spacing.small))
                    Column {
                        Text(
                            text = stringResource(Res.string.auth_server_connecting_will),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
                        Text(
                            text = stringResource(Res.string.auth_server_logout_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.auth_server_reset_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.auth_server_reauth_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_connect),
            onClick = onConfirm
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss
        )
    )
}

@androidx.compose.runtime.Composable
private fun ServerInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@androidx.compose.runtime.Composable
private fun ServerConfirmationDialogPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ServerConfirmationDialog(
            config = ServerConfig(
                host = "192.168.1.100",
                port = 8000,
                protocol = "http",
                name = "My Server",
                isCloud = false,
            ),
            serverInfo = ServerInfo(
                name = "My Dokus Server",
                version = "1.0.0",
                environment = "production",
                status = tech.dokus.domain.config.ServerStatus.UP,
                features = listOf("invoicing", "expenses"),
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
