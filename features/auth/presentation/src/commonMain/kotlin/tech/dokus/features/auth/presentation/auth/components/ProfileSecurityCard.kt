package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_change_password
import tech.dokus.aura.resources.profile_sessions
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Security card: change password, active sessions.
 */
@Composable
internal fun SecurityCard(
    onChangePassword: () -> Unit,
    onMySessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsRow(
                label = stringResource(Res.string.profile_change_password),
                chevron = true,
                onClick = onChangePassword,
            )
            SettingsRow(
                label = stringResource(Res.string.profile_sessions),
                chevron = true,
                onClick = onMySessions,
            )
        }
    }
}

@Preview
@Composable
private fun SecurityCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SecurityCard(
            onChangePassword = {},
            onMySessions = {},
        )
    }
}
