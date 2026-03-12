package tech.dokus.app.screens.settings.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.WorkspaceSettingsScreen
import tech.dokus.app.viewmodel.WorkspaceSettingsAction
import tech.dokus.app.viewmodel.WorkspaceSettingsContainer
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsSuccess
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_saved_successfully
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun WorkspaceSettingsRoute(
    container: WorkspaceSettingsContainer = container()
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<WorkspaceSettingsSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    RegisterHomeShellTopBar(
        route = HomeDestination.WorkspaceDetails.route,
        config = remember {
            HomeShellTopBarConfig(mode = HomeShellTopBarMode.Transparent)
        }
    )

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            WorkspaceSettingsSuccess.SettingsSaved ->
                stringResource(Res.string.settings_saved_successfully)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
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
            is WorkspaceSettingsAction.ShowSuccess -> {
                pendingSuccess = action.success
            }

            is WorkspaceSettingsAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    // Load settings on first composition
    LaunchedEffect(Unit) {
        container.store.intent(WorkspaceSettingsIntent.Load)
    }

    WorkspaceSettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onNavigateToPeppol = { navController.navigateTo(SettingsDestination.PeppolRegistration) },
    )
}
