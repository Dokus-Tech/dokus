package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_server_change
import tech.dokus.aura.resources.profile_server_dokus_cloud
import tech.dokus.aura.resources.profile_server_label
import tech.dokus.aura.resources.profile_server_url
import tech.dokus.aura.resources.profile_server_version
import tech.dokus.domain.config.ServerConfig
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Server connection card with DokusLabel header + StatusDot + SettingsRows.
 */
@Composable
internal fun ServerCard(
    currentServer: ServerConfig,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cloudName = stringResource(Res.string.profile_server_dokus_cloud)
    val serverName = currentServer.name ?: if (currentServer.isCloud) cloudName else currentServer.host

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            // Header with label + green pulse dot
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DokusLabel(text = stringResource(Res.string.profile_server_label))
                StatusDot(type = StatusDotType.Confirmed, pulse = true)
            }

            SettingsRow(label = stringResource(Res.string.profile_server_label), value = serverName)
            SettingsRow(
                label = stringResource(Res.string.profile_server_url),
                value = currentServer.baseUrl
                    .removePrefix("https://")
                    .removePrefix("http://"),
                mono = true
            )
            if (currentServer.version != null) {
                SettingsRow(label = stringResource(Res.string.profile_server_version), value = currentServer.version!!, mono = true)
            }
            SettingsRow(
                label = stringResource(Res.string.profile_server_change),
                chevron = true,
                showDivider = !currentServer.isCloud,
                onClick = onChangeServer,
            )
        }
    }
}

@Preview
@Composable
private fun ServerCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ServerCard(
            currentServer = ServerConfig.Cloud,
            onChangeServer = {},
            onResetToCloud = {},
        )
    }
}
