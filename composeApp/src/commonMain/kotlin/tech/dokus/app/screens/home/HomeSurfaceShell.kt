package tech.dokus.app.screens.home

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
import tech.dokus.app.navigation.HomeNavigationEnvelope
import tech.dokus.app.navigation.SearchFocusRequestBus
import tech.dokus.app.navigation.executeHomeNavigationCommand
import tech.dokus.app.navigation.local.HomeNavControllerProvided
import tech.dokus.app.screens.HomeNavHost
import tech.dokus.app.screens.HomeScreen
import tech.dokus.app.screens.dispatchProfileNavigation
import tech.dokus.app.screens.rememberFallbackShellTopBarConfig
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_more
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarHost
import tech.dokus.foundation.app.shell.LocalHomeShellTopBarHost
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.UserAccessContext
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.navigateToTopLevelTab

/**
 * Shared shell composable for both CM and BC surfaces.
 * All surface-specific behaviour is driven by [config].
 */
@Composable
internal fun HomeSurfaceShell(
    config: HomeSurfaceConfig,
    appModules: List<AppModule>,
    rootNavController: NavController,
    isLargeScreen: Boolean,
    shellState: HomeState.Ready,
    tenant: Tenant?,
    profileData: HomeShellProfileData?,
    pendingHomeCommand: HomeNavigationEnvelope?,
    onConsumeHomeCommand: (Long) -> Unit,
    onSwitchSurface: () -> Unit,
    onLogoutClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    // --- Nav providers & controller ---
    val homeNavProviders = remember(appModules) { appModules.homeNavigationProviders }
    val homeNavController = rememberNavController()
    val allNavItems = remember(appModules) { appModules.allNavItems }
    val navSections = remember(appModules) { appModules.navSectionsCombined }
    val desktopPinnedItems = remember(appModules) { appModules.desktopPinnedItems }
    val sortedRoutes = remember(allNavItems) { buildSortedRoutes(allNavItems) }

    // --- Top bar host ---
    val registeredTopBarConfigs = remember { mutableStateMapOf<String, HomeShellTopBarConfig>() }
    val topBarHost = remember(allNavItems, sortedRoutes) {
        object : HomeShellTopBarHost {
            override fun update(route: String, config: HomeShellTopBarConfig) {
                val normalized = normalizeRoute(route, sortedRoutes) ?: return
                if (registeredTopBarConfigs[normalized] == config) return
                registeredTopBarConfigs[normalized] = config
            }

            override fun clear(route: String) {
                val normalized = normalizeRoute(route, sortedRoutes) ?: return
                registeredTopBarConfigs.remove(normalized)
            }
        }
    }

    // --- Current route ---
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedRoute = normalizeRoute(currentRoute, sortedRoutes)

    // --- Access context (surface-specific drill-down) ---
    val surfaceAvailability = shellState.surfaceAvailability
    val isBCDrillDown = remember(currentRoute) { config.computeIsBCDrillDown(currentRoute) }
    val accessContext = remember(surfaceAvailability, tenant?.role, isBCDrillDown) {
        UserAccessContext(
            canCompanyManager = surfaceAvailability?.canCompanyManager ?: true,
            canBookkeeperConsole = surfaceAvailability?.canBookkeeperConsole ?: false,
            isSurfaceAvailabilityResolved = surfaceAvailability != null,
            currentTenantRole = tenant?.role,
            isBookkeeperConsoleDrillDown = isBCDrillDown,
        )
    }
    val canBCAccess = remember(accessContext.isSurfaceAvailabilityResolved, accessContext.canBookkeeperConsole) {
        !accessContext.isSurfaceAvailabilityResolved || accessContext.canBookkeeperConsole
    }

    // --- Command dispatch ---
    LaunchedEffect(
        pendingHomeCommand?.id,
        homeNavController,
        canBCAccess,
        accessContext.canCompanyManager,
    ) {
        val pending = pendingHomeCommand ?: return@LaunchedEffect
        val action = config.handleCommand(
            pending.command,
            canBCAccess,
            accessContext.canCompanyManager,
        )
        when (action) {
            SurfaceCommandAction.SwitchSurface -> {
                onSwitchSurface()
                return@LaunchedEffect
            }

            SurfaceCommandAction.ExecuteLocally -> {
                homeNavController.executeHomeNavigationCommand(
                    command = pending.command,
                    canBCAccess = canBCAccess,
                )
                onConsumeHomeCommand(pending.id)
            }
        }
    }

    // --- Visible items ---
    val visibleNavItems = remember(allNavItems, accessContext) {
        config.filterNavItems(allNavItems, accessContext)
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
        val baseTabs = visibleNavItems
            .filter { it.mobileTabOrder != null }
            .sortedBy { it.mobileTabOrder }
            .map { MobileTabConfig(it.id, it.titleRes, it.iconRes, it.destination) }
        if (config.appendMoreTab) {
            baseTabs + MobileTabConfig(
                "more",
                Res.string.nav_more,
                Res.drawable.more_horizontal,
                HomeDestination.More,
            )
        } else {
            baseTabs
        }
    }
    val hasSearch = remember(visibleNavItems) {
        visibleNavItems.any { it.destination == HomeDestination.Search }
    }
    val startDestination = remember(visibleNavItems, allNavItems) {
        config.resolveStartDestination(visibleNavItems, allNavItems)
    }

    // --- Top-level navigate (with surface intercept) ---
    val onTopLevelNavigate = remember(homeNavController, accessContext) {
        { destination: NavigationDestination ->
            if (destination == config.interceptDestination && config.shouldIntercept(accessContext)) {
                onSwitchSurface()
            } else {
                homeNavController.navigateToTopLevelTab(destination)
            }
        }
    }

    // --- Top bar config ---
    val fallbackShellTopBarConfig = rememberFallbackShellTopBarConfig(
        normalizedRoute = normalizedRoute,
        allNavItems = visibleNavItems,
    )
    val topBarConfig = resolveHomeShellTopBarConfig(
        route = currentRoute,
        allNavItems = visibleNavItems,
        sortedRoutes = sortedRoutes,
        registeredConfigs = registeredTopBarConfigs,
        fallback = { _, _ -> fallbackShellTopBarConfig },
    )

    // --- Nav host content ---
    val navHostContent: @Composable () -> Unit = {
        HomeNavControllerProvided(homeNavController) {
            CompositionLocalProvider(
                LocalHomeShellTopBarHost provides topBarHost,
                LocalUserAccessContext provides accessContext,
            ) {
                HomeNavHost(
                    navHostController = homeNavController,
                    homeNavProviders = homeNavProviders,
                    startDestination = startDestination,
                )
            }
        }
    }

    // --- Render shell ---
    HomeScreen(
        navSections = visibleNavSections,
        mobileTabs = mobileTabs,
        selectedRoute = currentRoute,
        topBarConfig = topBarConfig,
        desktopPinnedItems = visiblePinnedItems,
        tenantState = shellState.tenantState,
        profileData = profileData,
        isLoggingOut = shellState.isLoggingOut,
        snackbarHostState = snackbarHostState,
        onWorkspaceClick = remember(rootNavController) {
            { rootNavController.navigateTo(AuthDestination.WorkspaceSelect) }
        },
        onProfileClick = remember(isLargeScreen, homeNavController, rootNavController) {
            {
                dispatchProfileNavigation(
                    isLargeScreen = isLargeScreen,
                    onNavigateHomeProfile = {
                        homeNavController.navigateToTopLevelTab(HomeDestination.Profile)
                    },
                    onNavigateRootProfile = {
                        rootNavController.navigateTo(AuthDestination.ProfileSettings)
                    },
                )
            }
        },
        onAppearanceClick = remember(rootNavController) {
            { rootNavController.navigateTo(SettingsDestination.AppearanceSettings) }
        },
        onLogoutClick = onLogoutClick,
        onNavItemClick = remember(onTopLevelNavigate) {
            { navItem: tech.dokus.foundation.aura.model.NavItem ->
                onTopLevelNavigate(navItem.destination)
                if (navItem.destination == HomeDestination.Search) {
                    SearchFocusRequestBus.requestFocus()
                }
            }
        },
        onTabClick = remember(onTopLevelNavigate) {
            { tab: MobileTabConfig ->
                tab.destination?.let { destination ->
                    onTopLevelNavigate(destination)
                    if (destination == HomeDestination.Search) {
                        SearchFocusRequestBus.requestFocus()
                    }
                }
            }
        },
        onSearchShortcut = remember(hasSearch, onTopLevelNavigate) {
            {
                if (hasSearch) {
                    onTopLevelNavigate(HomeDestination.Search)
                    SearchFocusRequestBus.requestFocus()
                }
            }
        },
        content = navHostContent,
    )
}
