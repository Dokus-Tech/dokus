package tech.dokus.app.screens.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.app.navigation.HomeNavigationEnvelope
import tech.dokus.app.navigation.HomeNavigationSource
import tech.dokus.app.screens.filterCMNavItems
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.AppModule
import tech.dokus.navigation.destinations.HomeDestination

// ── CM (Company Manager) surface config ─────────────────────────────────

private val CMSurfaceConfig = HomeSurfaceConfig(
    computeIsBCDrillDown = { false },
    filterNavItems = ::filterCMNavItems,
    resolveStartDestination = { visible, all ->
        visible.firstOrNull { it.id == "today" }?.destination
            ?: visible.firstOrNull()?.destination
            ?: all.first().destination
    },
    appendMoreTab = true,
    handleCommand = { command, canBCAccess, _ ->
        when (command) {
            HomeNavigationCommand.OpenConsoleClients -> {
                if (canBCAccess) SurfaceCommandAction.SwitchSurface
                else SurfaceCommandAction.ExecuteLocally
            }

            is HomeNavigationCommand.OpenDocuments -> {
                if (command.source == HomeNavigationSource.BC && canBCAccess) {
                    SurfaceCommandAction.SwitchSurface
                } else {
                    SurfaceCommandAction.ExecuteLocally
                }
            }

            is HomeNavigationCommand.OpenDocumentReview -> SurfaceCommandAction.ExecuteLocally
        }
    },
    interceptDestination = HomeDestination.Accountant,
    shouldIntercept = { it.canBookkeeperConsole || !it.isSurfaceAvailabilityResolved },
)

// ── CM route wrapper ────────────────────────────────────────────────────

@Composable
internal fun CMHomeRoute(
    appModules: List<AppModule>,
    rootNavController: NavController,
    isLargeScreen: Boolean,
    shellState: HomeState.Ready,
    tenant: Tenant?,
    profileData: HomeShellProfileData?,
    pendingHomeCommand: HomeNavigationEnvelope?,
    onConsumeHomeCommand: (Long) -> Unit,
    onSwitchToBC: () -> Unit,
    onLogoutClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    HomeSurfaceShell(
        config = CMSurfaceConfig,
        appModules = appModules,
        rootNavController = rootNavController,
        isLargeScreen = isLargeScreen,
        shellState = shellState,
        tenant = tenant,
        profileData = profileData,
        pendingHomeCommand = pendingHomeCommand,
        onConsumeHomeCommand = onConsumeHomeCommand,
        onSwitchSurface = onSwitchToBC,
        onLogoutClick = onLogoutClick,
        snackbarHostState = snackbarHostState,
    )
}
