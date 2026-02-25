package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_checking_server
import tech.dokus.aura.resources.auth_connecting
import tech.dokus.aura.resources.auth_host_label
import tech.dokus.aura.resources.auth_onboarding_connect_server_subtitle
import tech.dokus.aura.resources.auth_onboarding_connect_server_title
import tech.dokus.aura.resources.auth_onboarding_use_cloud_instead
import tech.dokus.aura.resources.auth_port_label
import tech.dokus.aura.resources.auth_protocol_label
import tech.dokus.aura.resources.auth_validate_connection
import tech.dokus.aura.resources.auth_validating
import tech.dokus.aura.resources.auth_login_link
import tech.dokus.domain.config.ServerConfig
import tech.dokus.features.auth.mvi.ServerConnectionIntent
import tech.dokus.features.auth.mvi.ServerConnectionState
import tech.dokus.features.auth.presentation.auth.components.ProtocolSelector
import tech.dokus.features.auth.presentation.auth.components.ServerConfirmationDialog
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingSplitShell
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun ServerConnectionScreen(
    state: ServerConnectionState,
    currentServer: ServerConfig?,
    onIntent: (ServerConnectionIntent) -> Unit,
) {
    OnboardingSplitShell(
        brandVariant = OnboardingBrandVariant.Primary,
        modifier = Modifier.dismissKeyboardOnTapOutside()
    ) {
        SectionTitle(
            text = stringResource(Res.string.auth_onboarding_connect_server_title),
            horizontalArrangement = Arrangement.Start,
            onBackPress = { onIntent(ServerConnectionIntent.BackClicked) },
            right = {
                Text(
                    text = stringResource(Res.string.auth_login_link),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )

        Text(
            text = stringResource(Res.string.auth_onboarding_connect_server_subtitle),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        Text(
            text = stringResource(Res.string.auth_protocol_label),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        ProtocolSelector(
            selectedProtocol = state.protocol,
            onProtocolSelected = { onIntent(ServerConnectionIntent.UpdateProtocol(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        val hostError = (state as? ServerConnectionState.Input)?.hostError
        PTextFieldStandard(
            fieldName = stringResource(Res.string.auth_host_label),
            value = state.host,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            error = hostError,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(ServerConnectionIntent.UpdateHost(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        val portError = (state as? ServerConnectionState.Input)?.portError
        PTextFieldStandard(
            fieldName = stringResource(Res.string.auth_port_label),
            value = state.port,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            error = portError,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(ServerConnectionIntent.UpdatePort(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        PPrimaryButton(
            text = when (state) {
                is ServerConnectionState.Validating -> stringResource(Res.string.auth_validating)
                is ServerConnectionState.Connecting -> stringResource(Res.string.auth_connecting)
                else -> stringResource(Res.string.auth_validate_connection)
            },
            enabled = state !is ServerConnectionState.Validating && state !is ServerConnectionState.Connecting,
            onClick = { onIntent(ServerConnectionIntent.ValidateClicked) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (state is ServerConnectionState.Validating || state is ServerConnectionState.Connecting) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.small))
            Text(
                text = if (state is ServerConnectionState.Validating) {
                    stringResource(Res.string.auth_checking_server)
                } else {
                    stringResource(Res.string.auth_connecting)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val errorState = state as? ServerConnectionState.Error
        if (errorState != null) {
            Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
            DokusErrorContent(
                exception = errorState.exception,
                retryHandler = errorState.retryHandler,
                compact = true,
            )
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        TextButton(
            onClick = { onIntent(ServerConnectionIntent.ResetToCloud) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.auth_onboarding_use_cloud_instead),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    val previewState = state as? ServerConnectionState.Preview
    if (previewState != null) {
        ServerConfirmationDialog(
            config = previewState.config,
            serverInfo = previewState.serverInfo,
            onConfirm = { onIntent(ServerConnectionIntent.ConfirmConnection) },
            onDismiss = { onIntent(ServerConnectionIntent.CancelPreview) },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ServerConnectionScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ServerConnectionScreen(
            state = ServerConnectionState.Input(),
            currentServer = ServerConfig.Cloud,
            onIntent = {},
        )
    }
}
