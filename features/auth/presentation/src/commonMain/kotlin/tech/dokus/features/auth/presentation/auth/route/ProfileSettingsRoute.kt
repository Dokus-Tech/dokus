package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.ProfileSettingsAction
import tech.dokus.features.auth.mvi.ProfileSettingsContainer
import tech.dokus.features.auth.mvi.ProfileSettingsIntent
import tech.dokus.features.auth.mvi.MySessionsAction
import tech.dokus.features.auth.mvi.MySessionsContainer
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.presentation.auth.screen.ProfileDetailPaneHost
import tech.dokus.features.auth.presentation.auth.screen.ProfileDetailSelection
import tech.dokus.features.auth.presentation.auth.screen.ProfileSettingsScreen
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.platform.Logger
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_settings_title

@Composable
fun ProfileSettingsRoute(
    container: ProfileSettingsContainer = container(),
    logoutUseCase: LogoutUseCase = koinInject(),
    connectToServerUseCase: ConnectToServerUseCase = koinInject(),
    serverConfigManager: ServerConfigManager = koinInject()
) {
    val logger = remember { Logger.withTag("ProfileSettingsRoute") }
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val currentServer by serverConfigManager.currentServer.collectAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge

    var isLoggingOut by remember { mutableStateOf(false) }
    var activeDetailPane by remember { mutableStateOf<ProfileDetailSelection>(ProfileDetailSelection.None) }
    var resetToCloudError by remember { mutableStateOf<DokusException?>(null) }

    LaunchedEffect(isLargeScreen) {
        if (!isLargeScreen) {
            activeDetailPane = ProfileDetailSelection.None
        }
    }

    val profileTitle = stringResource(Res.string.profile_settings_title)
    RegisterHomeShellTopBar(
        route = HomeDestination.Profile.route,
        config = remember(isLargeScreen, profileTitle) {
            if (!isLargeScreen) {
                null
            } else {
                HomeShellTopBarConfig(
                    mode = HomeShellTopBarMode.Title(title = profileTitle),
                )
            }
        }
    )

    val state by container.store.subscribe { action ->
        when (action) {
            ProfileSettingsAction.NavigateToChangePassword -> navController.navigateTo(AuthDestination.ChangePassword)
            ProfileSettingsAction.NavigateToMySessions -> {
                if (isLargeScreen) {
                    activeDetailPane = ProfileDetailSelection.Sessions
                } else {
                    navController.navigateTo(AuthDestination.MySessions)
                }
            }
            ProfileSettingsAction.NavigateBack -> navController.navigateUp()
        }
    }

    var sessionsStateHolder: tech.dokus.features.auth.mvi.MySessionsState? = null
    var sessionsOnIntent: ((MySessionsIntent) -> Unit)? = null
    if (isLargeScreen) {
        val sessionsContainer: MySessionsContainer = container()
        val sessionsState by sessionsContainer.store.subscribe(DefaultLifecycle) { action ->
            when (action) {
                MySessionsAction.NavigateBack -> activeDetailPane = ProfileDetailSelection.None
            }
        }
        sessionsStateHolder = sessionsState
        sessionsOnIntent = { intent -> sessionsContainer.store.intent(intent) }
    }
    val detailPaneContent: (@Composable () -> Unit)? =
        if (isLargeScreen) {
            {
                ProfileDetailPaneHost(
                    selection = activeDetailPane,
                    sessionsState = checkNotNull(sessionsStateHolder),
                    onSessionsIntent = checkNotNull(sessionsOnIntent),
                )
            }
        } else {
            null
        }

    ProfileSettingsScreen(
        state = state,
        currentServer = currentServer,
        isLoggingOut = isLoggingOut,
        resetToCloudError = resetToCloudError,
        onDismissResetToCloudError = { resetToCloudError = null },
        onIntent = { container.store.intent(it) },
        onResendVerification = { container.store.intent(ProfileSettingsIntent.ResendVerificationClicked) },
        onChangePassword = { container.store.intent(ProfileSettingsIntent.ChangePasswordClicked) },
        onMySessions = {
            if (isLargeScreen) {
                activeDetailPane = if (activeDetailPane is ProfileDetailSelection.Sessions) {
                    ProfileDetailSelection.None
                } else {
                    ProfileDetailSelection.Sessions
                }
            } else {
                container.store.intent(ProfileSettingsIntent.MySessionsClicked)
            }
        },
        onChangeServer = { navController.navigateTo(AuthDestination.ServerConnection()) },
        onResetToCloud = {
            scope.launch {
                connectToServerUseCase.resetToCloud().fold(
                    onSuccess = {
                        navController.navigateTo(AuthDestination.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to reset to cloud" }
                        resetToCloudError = DokusException.Unknown(error)
                    }
                )
            }
        },
        onLogout = {
            scope.launch {
                isLoggingOut = true
                logger.d { "Logging out user" }
                logoutUseCase().fold(
                    onSuccess = {
                        logger.i { "Logout successful" }
                        navController.navigateTo(AuthDestination.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Logout failed" }
                        isLoggingOut = false
                    }
                )
            }
        },
        detailPaneContent = detailPaneContent
    )
}
