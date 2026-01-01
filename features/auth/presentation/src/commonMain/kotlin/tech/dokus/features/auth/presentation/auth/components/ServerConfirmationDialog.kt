package tech.dokus.features.auth.presentation.auth.components

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
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerInfo
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

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
@Composable
fun ServerConfirmationDialog(
    config: ServerConfig,
    serverInfo: ServerInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.auth_server_found),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column {
                // Server info card
                DokusCardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = DokusCardVariant.Soft,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ServerInfoRow(stringResource(Res.string.auth_server_name_label), serverInfo.name)
                        Spacer(modifier = Modifier.height(8.dp))
                        ServerInfoRow(stringResource(Res.string.auth_server_version), serverInfo.version)
                        Spacer(modifier = Modifier.height(8.dp))
                        ServerInfoRow(stringResource(Res.string.auth_server_url), config.baseUrl)
                        if (serverInfo.features.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ServerInfoRow(
                                stringResource(Res.string.auth_server_features_label),
                                serverInfo.features.joinToString(", ")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(Res.string.auth_server_connecting_will),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
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
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.action_connect),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.action_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
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
