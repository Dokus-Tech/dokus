package tech.dokus.app.screens.bc

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.app.navigation.HomeNavigationEnvelope
import tech.dokus.app.navigation.HomeNavigationSource
import tech.dokus.app.screens.home.HomeSurfaceConfig
import tech.dokus.app.screens.home.HomeSurfaceShell
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.app.screens.home.SurfaceCommandAction
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.shell.UserAccessContext
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route

// ── BC (Bookkeeper Console) surface config ──────────────────────────────

private val BCSurfaceConfig = HomeSurfaceConfig(
    computeIsBCDrillDown = { currentRoute ->
        val route = currentRoute?.substringBefore("?")
        route != null && route != HomeDestination.Accountant.route
    },
    filterNavItems = ::filterBCNavItems,
    resolveStartDestination = { visible, all ->
        HomeDestination.Accountant.takeIf { dest ->
            visible.any { it.destination == dest }
        } ?: visible.firstOrNull()?.destination
            ?: all.first().destination
    },
    appendMoreTab = false,
    handleCommand = { command, _, canCM ->
        when (command) {
            HomeNavigationCommand.OpenConsoleClients -> SurfaceCommandAction.ExecuteLocally

            is HomeNavigationCommand.OpenDocuments -> {
                if (command.source == HomeNavigationSource.CM && canCM) {
                    SurfaceCommandAction.SwitchSurface
                } else {
                    SurfaceCommandAction.ExecuteLocally
                }
            }

            is HomeNavigationCommand.OpenDocumentReview -> SurfaceCommandAction.ExecuteLocally
        }
    },
    interceptDestination = HomeDestination.Today,
    shouldIntercept = { it.canCompanyManager },
)

// ── BC route wrapper ────────────────────────────────────────────────────

@Composable
internal fun BCHomeRoute(
    appModules: List<AppModule>,
    rootNavController: NavController,
    isLargeScreen: Boolean,
    shellState: HomeState.Ready,
    tenant: Tenant?,
    profileData: HomeShellProfileData?,
    pendingHomeCommand: HomeNavigationEnvelope?,
    onConsumeHomeCommand: (Long) -> Unit,
    onSwitchToCM: () -> Unit,
    onLogoutClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    HomeSurfaceShell(
        config = BCSurfaceConfig,
        appModules = appModules,
        rootNavController = rootNavController,
        isLargeScreen = isLargeScreen,
        shellState = shellState,
        tenant = tenant,
        profileData = profileData,
        pendingHomeCommand = pendingHomeCommand,
        onConsumeHomeCommand = onConsumeHomeCommand,
        onSwitchSurface = onSwitchToCM,
        onLogoutClick = onLogoutClick,
        snackbarHostState = snackbarHostState,
    )
}

// ── BC-specific nav filter ──────────────────────────────────────────────

private val BCDestinations = setOf(
    HomeDestination.Accountant,
    HomeDestination.Documents,
)

internal fun filterBCNavItems(
    items: List<NavItem>,
    accessContext: UserAccessContext,
): List<NavItem> {
    if (!accessContext.canBookkeeperConsole) return emptyList()
    val allowed = buildSet {
        addAll(BCDestinations)
        if (accessContext.canCompanyManager) {
            add(HomeDestination.Today)
        }
    }
    return items.filter { it.destination in allowed }
}
