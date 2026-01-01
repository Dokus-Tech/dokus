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
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_reset_to_cloud_failed
import tech.dokus.aura.resources.profile_save_success
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.ProfileSettingsAction
import tech.dokus.features.auth.mvi.ProfileSettingsContainer
import tech.dokus.features.auth.presentation.auth.screen.ProfileSettingsScreen
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import org.koin.compose.koinInject
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.foundation.platform.Logger
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import kotlinx.coroutines.launch

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

    var pendingSuccess by remember { mutableStateOf(false) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val saveSuccessMessage = if (pendingSuccess) {
        stringResource(Res.string.profile_save_success)
    } else {
        null
    }
    val resetToCloudFailedMessage = stringResource(Res.string.profile_reset_to_cloud_failed)
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

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ProfileSettingsAction.ShowSaveSuccess -> pendingSuccess = true
            is ProfileSettingsAction.ShowSaveError -> pendingError = action.error
            ProfileSettingsAction.NavigateBack -> navController.navigateUp()
        }
    }

    ProfileSettingsScreen(
        state = state,
        currentServer = currentServer,
        isLoggingOut = isLoggingOut,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
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
        }
    )
}
