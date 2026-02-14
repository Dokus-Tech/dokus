package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import tech.dokus.aura.resources.auth_email_label
import tech.dokus.aura.resources.auth_forgot_password_description
import tech.dokus.aura.resources.auth_forgot_password_title
import tech.dokus.aura.resources.auth_send_reset_link
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.ForgotPasswordIntent
import tech.dokus.features.auth.mvi.ForgotPasswordState
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldEmailDefaults
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }

    Scaffold { contentPadding ->
        ForgotPasswordContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .padding(contentPadding)
                .clickable(
                    indication = null,
                    interactionSource = mutableInteractionSource
                ) {
                    focusManager.clearFocus()
                }
        )
    }
}

@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldsError = state.exceptionIfError()
    val isSubmitting = state is ForgotPasswordState.Submitting
    val canSubmit = state.email.isValid && !isSubmitting

    Box(
        modifier
            .fillMaxSize()
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
                androidx.compose.material3.Text(
                    text = stringResource(Res.string.auth_forgot_password_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = stringResource(Res.string.auth_forgot_password_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                PTextFieldEmail(
                    fieldName = stringResource(Res.string.auth_email_label),
                    value = state.email,
                    keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    error = fieldsError.takeIf { it is DokusException.Validation.InvalidEmail },
                    onAction = { onIntent(ForgotPasswordIntent.SubmitClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { onIntent(ForgotPasswordIntent.UpdateEmail(it)) }
                )

                Spacer(Modifier.height(16.dp))

                PPrimaryButton(
                    text = stringResource(Res.string.auth_send_reset_link),
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                    onClick = { onIntent(ForgotPasswordIntent.SubmitClicked) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
