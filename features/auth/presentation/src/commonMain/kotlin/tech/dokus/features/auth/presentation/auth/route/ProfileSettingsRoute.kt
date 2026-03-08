package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.profile_verification_email_sent
import tech.dokus.aura.resources.profile_reset_to_cloud_failed
import tech.dokus.aura.resources.profile_save_success
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.aura.resources.profile_sessions_revoke_others_success
import tech.dokus.aura.resources.profile_session_revoked
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.features.auth.mvi.ProfileSettingsAction
import tech.dokus.features.auth.mvi.ProfileSettingsContainer
import tech.dokus.features.auth.mvi.ProfileSettingsIntent
import tech.dokus.features.auth.mvi.MySessionsAction
import tech.dokus.features.auth.mvi.MySessionsContainer
import tech.dokus.features.auth.presentation.auth.screen.ProfileSettingsScreen
import tech.dokus.features.auth.presentation.auth.screen.MySessionsContent
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.platform.Logger
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
fun ProfileSettingsRoute(
    container: ProfileSettingsContainer = container(),
    logoutUseCase: LogoutUseCase = koinInject(),
    connectToServerUseCase: ConnectToServerUseCase = koinInject(),
    serverConfigManager: ServerConfigManager = koinInject()
) {
    val logger = remember { Logger.withTag("ProfileSettingsRoute") }
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentServer by serverConfigManager.currentServer.collectAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge

    var pendingSuccess by remember { mutableStateOf(false) }
    var pendingVerificationSent by remember { mutableStateOf(false) }
    var pendingSessionsMessage by remember { mutableStateOf<String?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }
    var showSessionsPane by remember { mutableStateOf(false) }

    LaunchedEffect(isLargeScreen) {
        if (!isLargeScreen) {
            showSessionsPane = false
        }
    }

    val closeLabel = stringResource(Res.string.action_close)
    val sessionsTitle = stringResource(Res.string.profile_sessions_title)
    RegisterHomeShellTopBar(
        route = HomeDestination.Profile.route,
        config = remember(isLargeScreen, showSessionsPane, closeLabel, sessionsTitle) {
            when {
                !isLargeScreen -> null
                showSessionsPane -> HomeShellTopBarConfig(
                    mode = HomeShellTopBarMode.Title(title = sessionsTitle),
                    actions = listOf(
                        HomeShellTopBarAction.Text(
                            label = closeLabel,
                            onClick = { showSessionsPane = false }
                        )
                    )
                )

                else -> HomeShellTopBarConfig(mode = HomeShellTopBarMode.Transparent)
            }
        }
    )

    val saveSuccessMessage = if (pendingSuccess) {
        stringResource(Res.string.profile_save_success)
    } else {
        null
    }
    val resetToCloudFailedMessage = stringResource(Res.string.profile_reset_to_cloud_failed)
    val verificationSentMessage = stringResource(Res.string.profile_verification_email_sent)
    val sessionRevokedMessage = stringResource(Res.string.profile_session_revoked)
    val revokeOthersMessage = stringResource(Res.string.profile_sessions_revoke_others_success)
    val errorMessage = pendingError?.localized

    LaunchedEffect(saveSuccessMessage) {
        if (saveSuccessMessage != null) {
            snackbarHostState.showSnackbar(saveSuccessMessage)
            pendingSuccess = false
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    LaunchedEffect(pendingVerificationSent) {
        if (pendingVerificationSent) {
            snackbarHostState.showSnackbar(verificationSentMessage)
            pendingVerificationSent = false
        }
    }

    LaunchedEffect(pendingSessionsMessage) {
        if (pendingSessionsMessage != null) {
            snackbarHostState.showSnackbar(pendingSessionsMessage!!)
            pendingSessionsMessage = null
        }
    }

    val state by container.store.subscribe { action ->
        when (action) {
            ProfileSettingsAction.ShowSaveSuccess -> pendingSuccess = true
            is ProfileSettingsAction.ShowSaveError -> pendingError = action.error
            ProfileSettingsAction.ShowVerificationEmailSent -> pendingVerificationSent = true
            is ProfileSettingsAction.ShowVerificationEmailError -> pendingError = action.error
            is ProfileSettingsAction.ShowAvatarError -> pendingError = action.error
            ProfileSettingsAction.NavigateToChangePassword -> navController.navigateTo(AuthDestination.ChangePassword)
            ProfileSettingsAction.NavigateToMySessions -> {
                if (isLargeScreen) {
                    showSessionsPane = true
                } else {
                    navController.navigateTo(AuthDestination.MySessions)
                }
            }
            ProfileSettingsAction.NavigateBack -> navController.navigateUp()
        }
    }

    val sessionsContainer: MySessionsContainer = container()
    val sessionsState by sessionsContainer.store.subscribe(DefaultLifecycle) { action ->
        if (showSessionsPane) {
            when (action) {
                MySessionsAction.NavigateBack -> showSessionsPane = false
                MySessionsAction.ShowSessionRevoked -> pendingSessionsMessage = sessionRevokedMessage
                MySessionsAction.ShowRevokeOthersSuccess -> pendingSessionsMessage = revokeOthersMessage
                is MySessionsAction.ShowError -> pendingError = action.error
            }
        }
    }
    val detailPaneContent: (@Composable () -> Unit)? =
        if (isLargeScreen && showSessionsPane) {
            { MySessionsContent(state = sessionsState, onIntent = { sessionsContainer.store.intent(it) }) }
        } else {
            null
        }

    ProfileSettingsScreen(
        state = state,
        currentServer = currentServer,
        isLoggingOut = isLoggingOut,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onResendVerification = { container.store.intent(ProfileSettingsIntent.ResendVerificationClicked) },
        onChangePassword = { container.store.intent(ProfileSettingsIntent.ChangePasswordClicked) },
        onMySessions = { container.store.intent(ProfileSettingsIntent.MySessionsClicked) },
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
                        snackbarHostState.showSnackbar(resetToCloudFailedMessage)
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
