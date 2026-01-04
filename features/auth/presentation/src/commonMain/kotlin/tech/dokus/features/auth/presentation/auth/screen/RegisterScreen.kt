package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import tech.dokus.features.auth.presentation.auth.model.RegisterFormFields
import tech.dokus.features.auth.presentation.auth.model.RegisterPage
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.background.SpotlightEffect
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding

private val fieldsContentMinHeight = 280.dp

@Composable
internal fun RegisterScreen(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = {
                RegisterContent(
                    state = state,
                    onIntent = onIntent,
                    contentPadding = contentPadding,
                    onNavigateUp = onNavigateUp,
                    onNavigateToLogin = onNavigateToLogin,
                )
            },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun RegisterContent(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    onNavigateUp: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    val fieldsError = state.exceptionIfError()
    val isLoading = state is RegisterState.Registering

    // Create form fields from state
    val fields = RegisterFormFields(
        email = state.email,
        password = state.password,
        firstName = state.firstName,
        lastName = state.lastName
    )
    val pagerState = rememberPagerState(pageCount = { 2 })

    val onContinueClick = { page: RegisterPage ->
        when (page) {
            RegisterPage.Profile -> {
                scope.launch {
                    pagerState.animateScrollToPage(1)
                    focusManager.moveFocus(FocusDirection.Next)
                }
            }

            RegisterPage.Credentials -> {
                onIntent(RegisterIntent.RegisterClicked)
            }
        }
    }

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
        val currentPage = RegisterPage.fromIndex(pagerState.currentPage)
        val onBack: () -> Unit = when (currentPage) {
            RegisterPage.Profile -> {
                onNavigateUp
            }

            RegisterPage.Credentials -> {
                { scope.launch { pagerState.animateScrollToPage(0) } }
            }
        }
        val title = when (currentPage) {
            RegisterPage.Profile -> stringResource(Res.string.auth_register_profile_title)
            RegisterPage.Credentials -> stringResource(Res.string.auth_register_credentials_title)
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .withContentPadding(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SectionTitle(
                text = title,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.limitWidthCenteredContent(),
                onBackPress = onBack,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .heightIn(min = fieldsContentMinHeight)
                    .limitWidthCenteredContent(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier.limitWidthCenteredContent()
                ) { page ->
                    if (page == 0) {
                        RegisterProfileFields(
                            focusManager = focusManager,
                            error = fieldsError,
                            fields = fields,
                            onFieldsUpdate = { updatedFields ->
                                onIntent(RegisterIntent.UpdateFirstName(updatedFields.firstName))
                                onIntent(RegisterIntent.UpdateLastName(updatedFields.lastName))
                            },
                            onSubmit = { onContinueClick(RegisterPage.Profile) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RegisterCredentialsFields(
                            focusManager = focusManager,
                            error = fieldsError,
                            fields = fields,
                            onFieldsUpdate = { updatedFields ->
                                onIntent(RegisterIntent.UpdateEmail(updatedFields.email))
                                onIntent(RegisterIntent.UpdatePassword(updatedFields.password))
                            },
                            onRegisterClick = { onContinueClick(RegisterPage.Credentials) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            RegisterActionButton(
                page = RegisterPage.fromIndex(pagerState.currentPage),
                fields = fields,
                onContinueClick = { onContinueClick(RegisterPage.fromIndex(pagerState.currentPage)) },
                modifier = Modifier.limitWidthCenteredContent().fillMaxWidth(),
                isLoading = isLoading,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.limitWidthCenteredContent(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.auth_has_account_prefix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(Res.string.auth_login_link),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onNavigateToLogin()
                    }
                )
            }
        }
    }
}
