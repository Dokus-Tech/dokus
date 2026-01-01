package tech.dokus.features.auth.presentation.auth.components

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_change_server
import tech.dokus.aura.resources.auth_dokus_cloud
import tech.dokus.aura.resources.auth_reset_cloud
import tech.dokus.aura.resources.auth_server_connection
import tech.dokus.aura.resources.auth_server_label
import tech.dokus.aura.resources.auth_server_url
import tech.dokus.aura.resources.auth_server_version
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.domain.config.ServerConfig
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dns
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
 * Settings section showing the current server connection.
 *
 * Displays:
 * - Server name or "Dokus Cloud"
 * - Base URL
 * - "Change Server" button
 * - "Reset to Cloud" option (if on self-hosted)
 *
 * @param currentServer The current server configuration
 * @param onChangeServer Called when "Change Server" is clicked
 * @param onResetToCloud Called when "Reset to Cloud" is clicked
 * @param modifier Optional modifier
 */
@Composable
fun CurrentServerSection(
    currentServer: ServerConfig,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.auth_server_connection),
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = if (currentServer.isCloud) Icons.Default.Cloud else Icons.Default.Dns,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Server name
            ServerInfoRow(
                label = stringResource(Res.string.auth_server_label),
                value = currentServer.name ?: if (currentServer.isCloud) {
                    stringResource(Res.string.auth_dokus_cloud)
                } else {
                    currentServer.host
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Server URL
            ServerInfoRow(
                label = stringResource(Res.string.auth_server_url),
                value = currentServer.baseUrl
            )

            // Version if available
            if (currentServer.version != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ServerInfoRow(
                    label = stringResource(Res.string.auth_server_version),
                    value = currentServer.version!!
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change server button
            POutlinedButton(
                text = stringResource(Res.string.auth_change_server),
                onClick = onChangeServer,
                modifier = Modifier.fillMaxWidth()
            )

            // Reset to cloud option (only if not on cloud)
            if (!currentServer.isCloud) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onResetToCloud,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.auth_reset_cloud),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerInfoRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
