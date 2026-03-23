package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_danger_zone
import tech.dokus.aura.resources.profile_deactivate_account
import tech.dokus.aura.resources.profile_deactivate_warning
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.profile_version_footer
import tech.dokus.domain.config.appVersion
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Danger zone card with red label + deactivation action.
 */
@Composable
internal fun DangerZoneCard(
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                DokusLabel(
                    text = stringResource(Res.string.profile_danger_zone),
                    color = MaterialTheme.colorScheme.error
                )
            }
            Box(modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 8.dp)) {
                Text(
                    text = stringResource(Res.string.profile_deactivate_warning),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
            SettingsRow(
                label = stringResource(Res.string.profile_deactivate_account),
                destructive = true,
                showDivider = false,
            )
        }
    }
}

/**
 * Log out card: single destructive row.
 */
@Composable
internal fun LogOutCard(
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        SettingsRow(
            label = stringResource(Res.string.profile_logout),
            destructive = true,
            showDivider = false,
            onClick = if (isLoggingOut) null else onLogout,
        )
    }
}

/**
 * Version footer: "Dokus v0.1.0 · Core" centered mono textFaint.
 */
@Composable
internal fun VersionFooter(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.profile_version_footer, appVersion.versionName, "Core"),
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
        color = MaterialTheme.colorScheme.textFaint,
    )
}

@Preview
@Composable
private fun DangerZoneCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DangerZoneCard()
    }
}

@Preview
@Composable
private fun LogOutCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        LogOutCard(isLoggingOut = false, onLogout = {})
    }
}

@Preview
@Composable
private fun VersionFooterPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        VersionFooter()
    }
}
