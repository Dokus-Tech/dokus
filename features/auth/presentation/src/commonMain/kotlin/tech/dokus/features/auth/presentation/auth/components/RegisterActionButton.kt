package tech.dokus.features.auth.presentation.auth.components

import tech.dokus.features.auth.presentation.auth.model.RegisterFormFields
import tech.dokus.features.auth.presentation.auth.model.RegisterPage
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.auth_register_title
import tech.dokus.foundation.aura.components.PPrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RegisterActionButton(
    page: RegisterPage,
    fields: RegisterFormFields,
    onContinueClick: (page: RegisterPage) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = when (page) {
        RegisterPage.Profile -> stringResource(Res.string.action_continue)
        RegisterPage.Credentials -> stringResource(Res.string.auth_register_title)
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
