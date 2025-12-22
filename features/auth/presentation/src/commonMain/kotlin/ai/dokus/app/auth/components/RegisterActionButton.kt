package ai.dokus.app.auth.components

import ai.dokus.app.auth.model.RegisterFormFields
import ai.dokus.app.auth.model.RegisterPage
import ai.dokus.foundation.design.components.PPrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
internal fun RegisterActionButton(
    page: RegisterPage,
    fields: RegisterFormFields,
    onContinueClick: (page: RegisterPage) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = when (page) {
        RegisterPage.Profile -> "Continue"
        RegisterPage.Credentials -> "Create Account"
    }
    val namesAreValid = fields.namesAreValid
    val credentialsAreValid = fields.credentialsAreValid
    val canContinue by remember(page, fields) {
        derivedStateOf {
            when (page) {
                RegisterPage.Profile -> namesAreValid
                RegisterPage.Credentials -> credentialsAreValid
            }
        }
    }
    PPrimaryButton(
        text = text,
        enabled = canContinue,
        isLoading = isLoading,
        modifier = modifier,
    ) { onContinueClick(page) }
}
