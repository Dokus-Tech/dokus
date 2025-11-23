package ai.dokus.app.auth.screen

import ai.dokus.app.auth.components.RegisterActionButton
import ai.dokus.app.auth.components.RegisterCredentialsFields
import ai.dokus.app.auth.components.RegisterProfileFields
import ai.dokus.app.auth.model.RegisterFormFields
import ai.dokus.app.auth.model.RegisterPage
import ai.dokus.app.auth.viewmodel.RegisterViewModel
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.auth_has_account_prefix
import ai.dokus.app.resources.generated.auth_login_link
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.design.components.layout.TwoPaneContainer
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val fieldsContentMinHeight = 280.dp

@Composable
internal fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel()
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = { RegisterContent(viewModel, contentPadding) },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun RegisterContent(
    viewModel: RegisterViewModel,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val navController = LocalNavController.current
    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    // Handle navigation effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RegisterViewModel.Effect.NavigateToHome -> {
                    navController.replace(CoreDestination.Home)
                }
                is RegisterViewModel.Effect.NavigateToCompanySelect -> {
                    navController.replace(AuthDestination.CompanySelect)
                }
            }
        }
    }

    val state = viewModel.state.collectAsState()
    val fieldsError: DokusException? =
        (state.value as? RegisterViewModel.State.Error)?.exception

    var fields by remember(viewModel) { mutableStateOf(RegisterFormFields()) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val isLoading = state.value is RegisterViewModel.State.Loading

    val onContinueClick = { page: RegisterPage ->
        when (page) {
            RegisterPage.Profile -> {
                scope.launch { pagerState.animateScrollToPage(1) }
            }

            RegisterPage.Credentials -> {
                viewModel.register(
                    fields.email,
                    fields.password,
                    fields.firstName,
                    fields.lastName
                )
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
                { navController.navigateUp() }
            }

            RegisterPage.Credentials -> {
                { scope.launch { pagerState.animateScrollToPage(0) } }
            }
        }
        val title = when (currentPage) {
            RegisterPage.Profile -> "Create your profile"
            RegisterPage.Credentials -> "Set up credentials"
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
                            onFieldsUpdate = { fields = it },
                            onSubmit = { onContinueClick(RegisterPage.Profile) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        RegisterCredentialsFields(
                            focusManager = focusManager,
                            error = fieldsError,
                            fields = fields,
                            onFieldsUpdate = { fields = it },
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
                        navController.navigateTo(AuthDestination.Login)
                    }
                )
            }
        }
    }
}
