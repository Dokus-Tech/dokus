package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_has_account_prefix
import tech.dokus.aura.resources.auth_login_link
import tech.dokus.aura.resources.auth_register_credentials_title
import tech.dokus.aura.resources.auth_register_profile_title
import tech.dokus.features.auth.mvi.RegisterIntent
import tech.dokus.features.auth.mvi.RegisterState
import tech.dokus.features.auth.presentation.auth.components.RegisterActionButton
import tech.dokus.features.auth.presentation.auth.components.RegisterCredentialsFields
import tech.dokus.features.auth.presentation.auth.components.RegisterProfileFields
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingSplitShell
import tech.dokus.features.auth.presentation.auth.model.RegisterFormFields
import tech.dokus.features.auth.presentation.auth.model.RegisterPage
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constraints

private val FieldsContentMinHeight = 280.dp

@Composable
internal fun RegisterScreen(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val fieldsError = state.exceptionIfError()
    val isLoading = state is RegisterState.Registering

    val fields = RegisterFormFields(
        email = state.email,
        password = state.password,
        firstName = state.firstName,
        lastName = state.lastName,
    )

    val pagerState = rememberPagerState(pageCount = { 2 })

    val onContinueClick: (RegisterPage) -> Unit = { page ->
        when (page) {
            RegisterPage.Profile -> {
                scope.launch {
                    pagerState.animateScrollToPage(1)
                    focusManager.moveFocus(FocusDirection.Next)
                }
            }

            RegisterPage.Credentials -> onIntent(RegisterIntent.RegisterClicked)
        }
    }

    val currentPage = RegisterPage.fromIndex(pagerState.currentPage)

    val onBack: () -> Unit = when (currentPage) {
        RegisterPage.Profile -> onNavigateUp
        RegisterPage.Credentials -> {
            { scope.launch { pagerState.animateScrollToPage(0) } }
        }
    }

    OnboardingSplitShell(brandVariant = OnboardingBrandVariant.Alt) {
        SectionTitle(
            text = when (currentPage) {
                RegisterPage.Profile -> stringResource(Res.string.auth_register_profile_title)
                RegisterPage.Credentials -> stringResource(Res.string.auth_register_credentials_title)
            },
            horizontalArrangement = Arrangement.Start,
            onBackPress = onBack,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        Box(
            modifier = Modifier
                .heightIn(min = FieldsContentMinHeight)
                .fillMaxWidth(),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                if (page == 0) {
                    RegisterProfileFields(
                        focusManager = focusManager,
                        error = fieldsError,
                        fields = fields,
                        onFieldsUpdate = { updated ->
                            onIntent(RegisterIntent.UpdateFirstName(updated.firstName))
                            onIntent(RegisterIntent.UpdateLastName(updated.lastName))
                        },
                        onSubmit = { onContinueClick(RegisterPage.Profile) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    RegisterCredentialsFields(
                        focusManager = focusManager,
                        error = fieldsError,
                        fields = fields,
                        onFieldsUpdate = { updated ->
                            onIntent(RegisterIntent.UpdateEmail(updated.email))
                            onIntent(RegisterIntent.UpdatePassword(updated.password))
                        },
                        onRegisterClick = { onContinueClick(RegisterPage.Credentials) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        RegisterActionButton(
            page = currentPage,
            fields = fields,
            onContinueClick = onContinueClick,
            modifier = Modifier.fillMaxWidth(),
            isLoading = isLoading,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.auth_has_account_prefix),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.auth_login_link),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onNavigateToLogin),
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun RegisterScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        RegisterScreen(
            state = RegisterState.Idle(),
            onIntent = {},
            onNavigateUp = {},
            onNavigateToLogin = {},
        )
    }
}
