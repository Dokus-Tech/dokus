package tech.dokus.app.screens.accountant

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
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_clients_subtitle
import tech.dokus.aura.resources.console_clients_title
import tech.dokus.app.screens.console.canRenderConsoleContent
import tech.dokus.app.screens.console.isConsoleAccessDenied
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateToTopLevelTab

@Composable
internal fun ConsoleClientsRoute(
    providedContainer: ConsoleClientsContainer? = null,
) {
    val accessContext = LocalUserAccessContext.current
    val navController = LocalNavController.current

    LaunchedEffect(accessContext.isSurfaceAvailabilityResolved, accessContext.canBookkeeperConsole) {
        if (isConsoleAccessDenied(accessContext)) {
            navController.navigateToTopLevelTab(HomeDestination.Today)
        }
    }

    if (!canRenderConsoleContent(accessContext)) return

    val container = providedContainer ?: container<
        ConsoleClientsContainer,
        ConsoleClientsState,
        ConsoleClientsIntent,
        ConsoleClientsAction
        >()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ConsoleClientsAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    val topBarConfig = HomeShellTopBarConfig(
        mode = HomeShellTopBarMode.Title(
            title = stringResource(Res.string.console_clients_title),
            subtitle = stringResource(Res.string.console_clients_subtitle),
        )
    )
    RegisterHomeShellTopBar(
        route = HomeDestination.ConsoleClients.route,
        config = topBarConfig,
    )

    ConsoleClientsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
    )
}
