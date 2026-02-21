package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_verifying_email
import tech.dokus.aura.resources.auth_verify_email_continue
import tech.dokus.aura.resources.auth_verify_email_error
import tech.dokus.aura.resources.auth_verify_email_retry
import tech.dokus.aura.resources.auth_verify_email_success
import tech.dokus.aura.resources.auth_verify_email_title
import tech.dokus.features.auth.mvi.VerifyEmailState
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding

@Composable
internal fun VerifyEmailScreen(
    state: VerifyEmailState,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold { padding ->
        VerifyEmailContent(
            state = state,
            onContinue = onContinue,
            onRetry = onRetry,
            contentPadding = padding
        )
    }
}

@Composable
private fun VerifyEmailContent(
    state: VerifyEmailState,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .withContentPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.limitWidthCenteredContent(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.auth_verify_email_title),
                style = MaterialTheme.typography.headlineMedium
            )

            val message = when (state) {
                VerifyEmailState.Verifying -> stringResource(Res.string.auth_verifying_email)
                VerifyEmailState.Success -> stringResource(Res.string.auth_verify_email_success)
                is VerifyEmailState.Error -> stringResource(Res.string.auth_verify_email_error)
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (state) {
                VerifyEmailState.Verifying -> Unit
                VerifyEmailState.Success -> {
                    PPrimaryButton(
                        text = stringResource(Res.string.auth_verify_email_continue),
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is VerifyEmailState.Error -> {
                    POutlinedButton(
                        text = stringResource(Res.string.auth_verify_email_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PPrimaryButton(
                        text = stringResource(Res.string.auth_verify_email_continue),
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun VerifyEmailScreenSuccessPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        VerifyEmailScreen(
            state = VerifyEmailState.Success,
            onContinue = {},
            onRetry = {},
        )
    }
}
