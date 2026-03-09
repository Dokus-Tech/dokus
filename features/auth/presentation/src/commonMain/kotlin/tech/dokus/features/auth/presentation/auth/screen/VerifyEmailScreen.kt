package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_verifying_email
import tech.dokus.aura.resources.auth_verify_email_continue
import tech.dokus.aura.resources.auth_verify_email_error
import tech.dokus.aura.resources.auth_verify_email_retry
import tech.dokus.aura.resources.auth_verify_email_success
import tech.dokus.aura.resources.auth_verify_email_title
import tech.dokus.features.auth.mvi.VerifyEmailState
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingSplitShell
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun VerifyEmailScreen(
    state: VerifyEmailState,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
) {
    OnboardingSplitShell(brandVariant = OnboardingBrandVariant.Alt) {
        SectionTitle(
            text = stringResource(Res.string.auth_verify_email_title),
            horizontalArrangement = Arrangement.Center,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        val message = when {
            state.verification.isLoading() -> stringResource(Res.string.auth_verifying_email)
            state.verification.isSuccess() -> stringResource(Res.string.auth_verify_email_success)
            state.verification.isError() -> stringResource(Res.string.auth_verify_email_error)
            else -> ""
        }

        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        when {
            state.verification.isLoading() -> Unit
            state.verification.isSuccess() -> {
                PPrimaryButton(
                    text = stringResource(Res.string.auth_verify_email_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.verification.isError() -> {
                POutlinedButton(
                    text = stringResource(Res.string.auth_verify_email_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

                PPrimaryButton(
                    text = stringResource(Res.string.auth_verify_email_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun VerifyEmailScreenSuccessPreview(
    @PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        VerifyEmailScreen(
            state = VerifyEmailState(verification = DokusState.success(Unit)),
            onContinue = {},
            onRetry = {},
        )
    }
}
