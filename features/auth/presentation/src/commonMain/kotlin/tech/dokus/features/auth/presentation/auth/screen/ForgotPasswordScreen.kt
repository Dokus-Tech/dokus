package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_email_label
import tech.dokus.aura.resources.auth_forgot_password_description
import tech.dokus.aura.resources.auth_forgot_password_title
import tech.dokus.aura.resources.auth_send_reset_link
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.ForgotPasswordIntent
import tech.dokus.features.auth.mvi.ForgotPasswordState
import tech.dokus.features.auth.presentation.auth.components.v2.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.v2.OnboardingSplitShell
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldEmailDefaults
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val fieldsError = state.exceptionIfError()
    val isSubmitting = state is ForgotPasswordState.Submitting
    val canSubmit = state.email.isValid && !isSubmitting

    OnboardingSplitShell(brandVariant = OnboardingBrandVariant.Alt) {
        SectionTitle(
            text = stringResource(Res.string.auth_forgot_password_title),
            horizontalArrangement = Arrangement.Start,
            onBackPress = onNavigateUp,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        Text(
            text = stringResource(Res.string.auth_forgot_password_description),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        PTextFieldEmail(
            fieldName = stringResource(Res.string.auth_email_label),
            value = state.email,
            keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Done),
            error = fieldsError.takeIf { it is DokusException.Validation.InvalidEmail },
            onAction = { onIntent(ForgotPasswordIntent.SubmitClicked) },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(ForgotPasswordIntent.UpdateEmail(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        PPrimaryButton(
            text = stringResource(Res.string.auth_send_reset_link),
            enabled = canSubmit,
            isLoading = isSubmitting,
            onClick = { onIntent(ForgotPasswordIntent.SubmitClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ForgotPasswordScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ForgotPasswordScreen(
            state = ForgotPasswordState.Idle(),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}
