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
import tech.dokus.aura.resources.auth_password_label
import tech.dokus.aura.resources.auth_reset_password_button
import tech.dokus.aura.resources.auth_reset_password_description
import tech.dokus.aura.resources.auth_reset_password_title
import tech.dokus.aura.resources.profile_confirm_password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.NewPasswordIntent
import tech.dokus.features.auth.mvi.NewPasswordState
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingSplitShell
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside

@Composable
internal fun NewPasswordScreen(
    state: NewPasswordState,
    onIntent: (NewPasswordIntent) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val fieldsError = state.exceptionIfError()
    val isSubmitting = state is NewPasswordState.Submitting
    val passwordsMatch = state.password.value == state.passwordConfirmation.value
    val canSubmit = state.password.isValid && state.passwordConfirmation.isValid && passwordsMatch && !isSubmitting

    OnboardingSplitShell(
        brandVariant = OnboardingBrandVariant.Alt,
        modifier = Modifier.dismissKeyboardOnTapOutside()
    ) {
        SectionTitle(
            text = stringResource(Res.string.auth_reset_password_title),
            horizontalArrangement = Arrangement.Start,
            onBackPress = onNavigateUp,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        Text(
            text = stringResource(Res.string.auth_reset_password_description),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        PTextFieldPassword(
            fieldName = stringResource(Res.string.auth_password_label),
            value = state.password,
            keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            error = fieldsError.takeIf { it is DokusException.Validation.WeakPassword },
            onAction = {},
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(NewPasswordIntent.UpdatePassword(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        PTextFieldPassword(
            fieldName = stringResource(Res.string.profile_confirm_password),
            value = state.passwordConfirmation,
            keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(imeAction = ImeAction.Done),
            error = fieldsError.takeIf { it is DokusException.Validation.PasswordDoNotMatch },
            onAction = { onIntent(NewPasswordIntent.SubmitClicked) },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(NewPasswordIntent.UpdatePasswordConfirmation(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        PPrimaryButton(
            text = stringResource(Res.string.auth_reset_password_button),
            enabled = canSubmit,
            isLoading = isSubmitting,
            onClick = { onIntent(NewPasswordIntent.SubmitClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun NewPasswordScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        NewPasswordScreen(
            state = NewPasswordState.Idle(),
            onIntent = {},
            onNavigateUp = {},
        )
    }
}
