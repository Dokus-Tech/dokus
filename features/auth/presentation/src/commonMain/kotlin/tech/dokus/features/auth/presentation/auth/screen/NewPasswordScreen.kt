package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
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
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding

@Composable
internal fun NewPasswordScreen(
    state: NewPasswordState,
    onIntent: (NewPasswordIntent) -> Unit,
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                AmbientBackground()
            },
            left = {
                NewPasswordContent(
                    state = state,
                    onIntent = onIntent,
                    contentPadding = contentPadding
                )
            },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun NewPasswordContent(
    state: NewPasswordState,
    onIntent: (NewPasswordIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val fieldsError = state.exceptionIfError()
    val isSubmitting = state is NewPasswordState.Submitting
    val passwordsMatch = state.password.value == state.passwordConfirmation.value
    val canSubmit = state.password.isValid && state.passwordConfirmation.isValid && passwordsMatch && !isSubmitting

    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }

    Box(
        Modifier
            .padding(contentPadding)
            .clickable(
                indication = null,
                interactionSource = mutableInteractionSource
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .withContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.limitWidthCenteredContent(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(Res.string.auth_reset_password_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_reset_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                PTextFieldPassword(
                    fieldName = stringResource(Res.string.auth_password_label),
                    value = state.password,
                    keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Next
                    ),
                    error = fieldsError.takeIf { it is DokusException.Validation.WeakPassword },
                    onAction = {},
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { onIntent(NewPasswordIntent.UpdatePassword(it)) }
                )

                Spacer(Modifier.height(16.dp))

                PTextFieldPassword(
                    fieldName = stringResource(Res.string.profile_confirm_password),
                    value = state.passwordConfirmation,
                    keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    error = fieldsError.takeIf { it is DokusException.Validation.PasswordDoNotMatch },
                    onAction = { onIntent(NewPasswordIntent.SubmitClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { onIntent(NewPasswordIntent.UpdatePasswordConfirmation(it)) }
                )

                Spacer(Modifier.height(16.dp))

                PPrimaryButton(
                    text = stringResource(Res.string.auth_reset_password_button),
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                    onClick = { onIntent(NewPasswordIntent.SubmitClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
