package tech.dokus.app.screens.companymanager

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import tech.dokus.app.allNavItems
import tech.dokus.app.desktopPinnedItems
import tech.dokus.app.homeNavigationProviders
import tech.dokus.app.navSectionsCombined
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.app.navigation.HomeNavigationEnvelope
import tech.dokus.app.navigation.HomeNavigationSource
import tech.dokus.app.navigation.SearchFocusRequestBus
import tech.dokus.app.navigation.executeHomeNavigationCommand
import tech.dokus.app.navigation.local.HomeNavControllerProvided
import tech.dokus.app.screens.HomeNavHost
import tech.dokus.app.screens.dispatchProfileNavigation
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.app.screens.home.buildSortedRoutes
import tech.dokus.app.screens.home.normalizeRoute
import tech.dokus.app.screens.home.resolveHomeShellTopBarConfig
import tech.dokus.app.screens.rememberFallbackShellTopBarConfig
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarHost
import tech.dokus.foundation.app.shell.LocalHomeShellTopBarHost
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.UserAccessContext
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.navigateToTopLevelTab

@Composable
internal fun CompanyManagerHomeRoute(
    appModules: List<AppModule>,
    rootNavController: NavController,
    isLargeScreen: Boolean,
    shellState: HomeState.Ready,
    tenant: Tenant?,
    profileData: HomeShellProfileData?,
    pendingHomeCommand: HomeNavigationEnvelope?,
    onConsumeHomeCommand: (Long) -> Unit,
    onSwitchToWorkspaceSurface: () -> Unit,
    onLogoutClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val homeNavProviders = remember(appModules) { appModules.homeNavigationProviders }
    val homeNavController = rememberNavController()
    val allNavItems = remember(appModules) { appModules.allNavItems }
    val navSections = remember(appModules) { appModules.navSectionsCombined }
    val desktopPinnedItems = remember(appModules) { appModules.desktopPinnedItems }
    val sortedRoutes = remember(allNavItems) { buildSortedRoutes(allNavItems) }
    val registeredTopBarConfigs = remember { mutableStateMapOf<String, HomeShellTopBarConfig>() }
    val topBarHost = remember(allNavItems, sortedRoutes) {
        object : HomeShellTopBarHost {
            override fun update(route: String, config: HomeShellTopBarConfig) {
                val normalizedRoute = normalizeRoute(route, sortedRoutes) ?: return
                if (registeredTopBarConfigs[normalizedRoute] == config) return
                registeredTopBarConfigs[normalizedRoute] = config
            }

            override fun clear(route: String) {
                val normalizedRoute = normalizeRoute(route, sortedRoutes) ?: return
                registeredTopBarConfigs.remove(normalizedRoute)
            }
        }
    }
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedRoute = normalizeRoute(currentRoute, sortedRoutes)
    val surfaceAvailability = shellState.surfaceAvailability
    val isConsoleDrillDown = remember(currentRoute) {
        val route = currentRoute?.substringBefore("?")
        route != null && route != HomeDestination.Accountant.route
    }
    val accessContext = remember(surfaceAvailability, tenant?.role, isConsoleDrillDown) {
        UserAccessContext(
            canWorkspace = surfaceAvailability?.canWorkspace ?: true,
            canConsole = surfaceAvailability?.canConsole ?: false,
            isSurfaceAvailabilityResolved = surfaceAvailability != null,
            currentTenantRole = tenant?.role,
            isConsoleDrillDown = isConsoleDrillDown,
        )
    }
    val canConsoleAccess = remember(accessContext.isSurfaceAvailabilityResolved, accessContext.canConsole) {
        !accessContext.isSurfaceAvailabilityResolved || accessContext.canConsole
    }

    LaunchedEffect(
        pendingHomeCommand?.id,
        homeNavController,
        canConsoleAccess,
        accessContext.canWorkspace,
    ) {
        val pending = pendingHomeCommand ?: return@LaunchedEffect
        when (val command = pending.command) {
            HomeNavigationCommand.OpenConsoleClients -> {
                homeNavController.executeHomeNavigationCommand(command = command, canConsoleAccess = canConsoleAccess)
                onConsumeHomeCommand(pending.id)
            }

            is HomeNavigationCommand.OpenDocuments -> {
                if (command.source == HomeNavigationSource.Workspace && accessContext.canWorkspace) {
                    onSwitchToWorkspaceSurface()
                    return@LaunchedEffect
                }
                homeNavController.executeHomeNavigationCommand(command = command, canConsoleAccess = canConsoleAccess)
                onConsumeHomeCommand(pending.id)
            }

            is HomeNavigationCommand.OpenDocumentReview -> {
                homeNavController.executeHomeNavigationCommand(command = command, canConsoleAccess = canConsoleAccess)
                onConsumeHomeCommand(pending.id)
            }
        }
    }

    val visibleNavItems = remember(allNavItems, accessContext) {
        filterCompanyManagerNavItems(
            items = allNavItems,
            accessContext = accessContext
        )
    }
    val visibleNavIds = remember(visibleNavItems) { visibleNavItems.map { it.id }.toSet() }
    val visibleNavSections = remember(navSections, visibleNavIds) {
        navSections.mapNotNull { section ->
            val filteredItems = section.items.filter { it.id in visibleNavIds }
            if (filteredItems.isEmpty()) null else section.copy(items = filteredItems)
        }
    }
    val visiblePinnedItems = remember(desktopPinnedItems, visibleNavIds) {
        desktopPinnedItems.filter { it.id in visibleNavIds }
    }
    val mobileTabs = remember(visibleNavItems) {
        visibleNavItems
            .filter { it.mobileTabOrder != null }
            .sortedBy { it.mobileTabOrder }
            .map { MobileTabConfig(it.id, it.titleRes, it.iconRes, it.destination) }
    }
    val hasSearch = remember(visibleNavItems) {
        visibleNavItems.any { it.destination == HomeDestination.Search }
    }
    val startDestination = remember(visibleNavItems, allNavItems) {
        HomeDestination.Accountant.takeIf { destination ->
            visibleNavItems.any { it.destination == destination }
        } ?: visibleNavItems.firstOrNull()?.destination
            ?: allNavItems.first().destination
    }
    val onTopLevelNavigate = remember(homeNavController, accessContext.canWorkspace) {
        { destination: NavigationDestination ->
            if (destination == HomeDestination.Today && accessContext.canWorkspace) {
                onSwitchToWorkspaceSurface()
            } else {
                homeNavController.navigateToTopLevelTab(destination)
            }
        }
    }
    val fallbackShellTopBarConfig = rememberFallbackShellTopBarConfig(
        normalizedRoute = normalizedRoute,
        allNavItems = visibleNavItems,
    )
    val topBarConfig = resolveHomeShellTopBarConfig(
        route = currentRoute,
        allNavItems = visibleNavItems,
        sortedRoutes = sortedRoutes,
        registeredConfigs = registeredTopBarConfigs,
        fallback = { _, _ -> fallbackShellTopBarConfig }
    )
    val navHostContent: @Composable () -> Unit = {
        HomeNavControllerProvided(homeNavController) {
            CompositionLocalProvider(
                LocalHomeShellTopBarHost provides topBarHost,
                LocalUserAccessContext provides accessContext,
            ) {
                HomeNavHost(
                    navHostController = homeNavController,
                    homeNavProviders = homeNavProviders,
                    startDestination = startDestination
                )
            }
        }
    }

    CompanyManagerHomeScreen(
        navSections = visibleNavSections,
        mobileTabs = mobileTabs,
        selectedRoute = currentRoute,
        topBarConfig = topBarConfig,
        desktopPinnedItems = visiblePinnedItems,
        tenantState = shellState.tenantState,
        profileData = profileData,
        isLoggingOut = shellState.isLoggingOut,
        snackbarHostState = snackbarHostState,
        onWorkspaceClick = { rootNavController.navigateTo(AuthDestination.WorkspaceSelect) },
        onProfileClick = {
            dispatchProfileNavigation(
                isLargeScreen = isLargeScreen,
                onNavigateHomeProfile = {
                    homeNavController.navigateToTopLevelTab(HomeDestination.Profile)
                },
                onNavigateRootProfile = {
                    rootNavController.navigateTo(AuthDestination.ProfileSettings)
                }
            )
        },
        onAppearanceClick = { rootNavController.navigateTo(SettingsDestination.AppearanceSettings) },
        onLogoutClick = onLogoutClick,
        onNavItemClick = { navItem ->
            onTopLevelNavigate(navItem.destination)
            if (navItem.destination == HomeDestination.Search) {
                SearchFocusRequestBus.requestFocus()
            }
        },
        onTabClick = { tab ->
            tab.destination?.let { destination ->
                onTopLevelNavigate(destination)
                if (destination == HomeDestination.Search) {
                    SearchFocusRequestBus.requestFocus()
                }
            }
        },
        onSearchShortcut = {
            if (hasSearch) {
                onTopLevelNavigate(HomeDestination.Search)
                SearchFocusRequestBus.requestFocus()
            }
        },
        content = navHostContent,
    )
}

internal fun filterCompanyManagerNavItems(
    items: List<NavItem>,
    accessContext: UserAccessContext,
): List<NavItem> {
    if (!accessContext.canConsole) return emptyList()
    val allowedDestinations = buildSet {
        addAll(CompanyManagerDestinations)
        if (accessContext.canWorkspace) {
            add(HomeDestination.Today)
        }
    }
    return items.filter { it.destination in allowedDestinations }
}

private val CompanyManagerDestinations = setOf(
    HomeDestination.Accountant,
    HomeDestination.Documents,
)
