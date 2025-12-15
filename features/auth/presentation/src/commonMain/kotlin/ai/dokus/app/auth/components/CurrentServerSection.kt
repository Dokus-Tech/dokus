package ai.dokus.app.auth.components

import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.domain.config.ServerConfig
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Server Connection",
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
                label = "Server",
                value = currentServer.name ?: if (currentServer.isCloud) "Dokus Cloud" else currentServer.host
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Server URL
            ServerInfoRow(
                label = "URL",
                value = currentServer.baseUrl
            )

            // Version if available
            if (currentServer.version != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ServerInfoRow(
                    label = "Version",
                    value = currentServer.version!!
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Change server button
            POutlinedButton(
                text = "Change Server",
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
                        text = "Reset to Dokus Cloud",
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
