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
import tech.dokus.aura.resources.console_clients_count
import tech.dokus.aura.resources.console_clients_overview_title
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.app.screens.console.canRenderConsoleContent
import tech.dokus.app.screens.console.isConsoleAccessDenied
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
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

    val defaultTitle = stringResource(Res.string.console_clients_overview_title)
    val periodLabel = stringResource(Res.string.console_requests_period_label)
    val topBarContent = when (state) {
        is ConsoleClientsState.Content -> {
            val content = state as ConsoleClientsState.Content
            val selectedClient = content.clients.firstOrNull { it.tenantId == content.selectedClientTenantId }
            if (selectedClient != null) {
                selectedClient.companyName.value to (selectedClient.vatNumber?.formatted ?: "")
            } else {
                defaultTitle to "${content.firmName} · ${stringResource(Res.string.console_clients_count, content.clients.size)}"
            }
        }

        else -> defaultTitle to ""
    }
    val topBarConfig = HomeShellTopBarConfig(
        mode = HomeShellTopBarMode.Title(
            title = topBarContent.first,
            subtitle = topBarContent.second.ifBlank { null },
        ),
        actions = listOf(
            HomeShellTopBarAction.Text(
                label = periodLabel,
                onClick = {},
            )
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
