package tech.dokus.app.screens.accountant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_clients_count
import tech.dokus.aura.resources.console_clients_overview_title
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.app.screens.console.canRenderConsoleContent
import tech.dokus.app.screens.console.isConsoleAccessDenied
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.app.state.isSuccess
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

    val state by container.store.subscribe(DefaultLifecycle) { _ -> }

    val defaultTitle = stringResource(Res.string.console_clients_overview_title)
    val periodLabel = stringResource(Res.string.console_requests_period_label)
    val clients = state.clients
    val topBarTitle: String
    val topBarSubtitle: String
    if (clients.isSuccess()) {
        val selectedClient = clients.data.firstOrNull { it.tenantId == state.selectedClientTenantId }
        if (selectedClient != null) {
            topBarTitle = selectedClient.companyName.value
            topBarSubtitle = selectedClient.vatNumber?.formatted ?: ""
        } else {
            topBarTitle = defaultTitle
            topBarSubtitle = "${state.firmName.orEmpty()} · ${stringResource(Res.string.console_clients_count, clients.data.size)}"
        }
    } else {
        topBarTitle = defaultTitle
        topBarSubtitle = ""
    }
    val topBarConfig = HomeShellTopBarConfig(
        mode = HomeShellTopBarMode.Title(
            title = topBarTitle,
            subtitle = topBarSubtitle.ifBlank { null },
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
        onIntent = { container.store.intent(it) },
    )
}
