package tech.dokus.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.app.homeItems
import tech.dokus.app.homeNavigationProviders
import tech.dokus.app.navigation.NavDefinition
import tech.dokus.app.screens.home.DesktopSidebarBottomControls
import tech.dokus.app.screens.home.DesktopShellTopBar
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.app.screens.home.MobileShellTopBar
import tech.dokus.app.screens.home.resolveHomeShellTopBarConfig
import tech.dokus.app.viewmodel.HomeAction
import tech.dokus.app.viewmodel.HomeContainer
import tech.dokus.app.viewmodel.HomeIntent
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.User
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarHost
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.LocalHomeShellTopBarHost
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRailSectioned
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

/**
 * Home screen using FlowMVI Container pattern.
 * Main navigation shell containing bottom navigation (mobile) or navigation rail (desktop).
 */
@Composable
internal fun HomeScreen(
    appModules: List<AppModule> = LocalAppModules.current,
    container: HomeContainer = container(),
) {
    val navController = LocalNavController.current
    val homeNavProviders = remember(appModules) { appModules.homeNavigationProviders }
    val homeNavController = rememberNavController()
    val homeItems = remember(appModules) { appModules.homeItems }
    val startDestination = remember(homeItems) { homeItems.first().destination }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val errorMessage = pendingError?.localized
    val isLargeScreen = LocalScreenSize.current.isLarge
    var fallbackSearchQuery by rememberSaveable { mutableStateOf("") }
    var isMobileSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val registeredTopBarConfigs = remember { mutableStateMapOf<String, HomeShellTopBarConfig>() }
    val topBarHost = remember {
        object : HomeShellTopBarHost {
            override fun update(route: String, config: HomeShellTopBarConfig) {
                if (registeredTopBarConfigs[route] == config) return
                registeredTopBarConfigs[route] = config
            }

            override fun clear(route: String) {
                registeredTopBarConfigs.remove(route)
            }
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    LaunchedEffect(isLargeScreen) {
        isMobileSearchExpanded = isLargeScreen
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is HomeAction.ShowError -> pendingError = action.error
        }
    }

    // Notify container when screen appears
    LaunchedEffect(Unit) {
        container.store.intent(HomeIntent.ScreenAppeared)
    }

    // Get current route directly from backstack
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val normalizedRoute = NavDefinition.normalizeRoute(currentRoute)

    val shellState = state as? HomeState.Ready ?: HomeState.Ready()
    val tenant = (shellState.tenantState as? DokusState.Success<Tenant>)?.data
    val user = (shellState.userState as? DokusState.Success<User>)?.data
    val fallbackShellTopBarConfig = rememberFallbackShellTopBarConfig(
        normalizedRoute = normalizedRoute,
        fallbackSearchQuery = fallbackSearchQuery,
        onFallbackSearchQueryChange = { fallbackSearchQuery = it },
        isMobileSearchExpanded = isMobileSearchExpanded,
        onExpandSearch = { isMobileSearchExpanded = true }
    )
    val topBarConfig = resolveHomeShellTopBarConfig(
        route = currentRoute,
        registeredConfigs = registeredTopBarConfigs,
        fallback = { fallbackShellTopBarConfig }
    )
    val profileData = buildProfileData(
        user = user,
        tierLabel = tenant?.subscription?.localized
    )

    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                RailNavigationLayout(
                    selectedRoute = currentRoute,
                    topBarConfig = topBarConfig,
                    tenantState = shellState.tenantState,
                    profileData = profileData,
                    isLoggingOut = shellState.isLoggingOut,
                    onWorkspaceClick = { navController.navigateTo(AuthDestination.WorkspaceSelect) },
                    onProfileClick = { navController.navigateTo(AuthDestination.ProfileSettings) },
                    onAppearanceClick = { navController.navigateTo(SettingsDestination.AppearanceSettings) },
                    onLogoutClick = { container.store.intent(HomeIntent.Logout) },
                    onNavItemClick = { navItem ->
                        NavDefinition.routeToDestination(navItem.route)?.let { destination ->
                            homeNavController.navigateTo(destination)
                        }
                    },
                    content = {
                        CompositionLocalProvider(LocalHomeShellTopBarHost provides topBarHost) {
                            HomeNavHost(
                                navHostController = homeNavController,
                                homeNavProviders = homeNavProviders,
                                startDestination = startDestination
                            )
                        }
                    }
                )
            } else {
                BottomNavigationLayout(
                    selectedRoute = currentRoute,
                    topBarConfig = topBarConfig,
                    tenantState = shellState.tenantState,
                    profileData = profileData,
                    isLoggingOut = shellState.isLoggingOut,
                    onWorkspaceClick = { navController.navigateTo(AuthDestination.WorkspaceSelect) },
                    onProfileClick = { navController.navigateTo(AuthDestination.ProfileSettings) },
                    onAppearanceClick = { navController.navigateTo(SettingsDestination.AppearanceSettings) },
                    onLogoutClick = { container.store.intent(HomeIntent.Logout) },
                    onTabClick = { tab ->
                        tab.route?.let { route ->
                            NavDefinition.routeToDestination(route)?.let { destination ->
                                homeNavController.navigateTo(destination)
                            }
                        }
                    },
                    content = {
                        CompositionLocalProvider(LocalHomeShellTopBarHost provides topBarHost) {
                            HomeNavHost(
                                navHostController = homeNavController,
                                homeNavProviders = homeNavProviders,
                                startDestination = startDestination
                            )
                        }
                    }
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HomeNavHost(
    navHostController: NavHostController,
    homeNavProviders: List<NavigationProvider>,
    startDestination: NavigationDestination,
) {
    val transitionsProvider: TransitionsProvider = remember { TransitionsProvider.forTabs() }
    NavHost(
        navHostController,
        startDestination = startDestination,
        enterTransition = { with(transitionsProvider) { enterTransition } },
        exitTransition = { with(transitionsProvider) { exitTransition } },
        popEnterTransition = { with(transitionsProvider) { popEnterTransition } },
        popExitTransition = { with(transitionsProvider) { popExitTransition } },
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        homeNavProviders.forEach { navProvider ->
            with(navProvider) {
                registerGraph()
            }
        }
    }
}

@Composable
private fun RailNavigationLayout(
    selectedRoute: String?,
    topBarConfig: HomeShellTopBarConfig?,
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavItemClick: (NavItem) -> Unit,
    content: @Composable () -> Unit
) {
    // Track expanded sections (accordion behavior: only one expanded at a time)
    val expandedSections = remember {
        mutableStateMapOf<String, Boolean>().apply {
            NavDefinition.sections.forEach { section ->
                put(section.id, section.defaultExpanded)
            }
        }
    }

    // Detached, calm desktop shell (Revolut structure × Perplexity calm)
    Row(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Detached glass rail panel
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(240.dp),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                AppNameText(modifier = Modifier.padding(bottom = 24.dp))

                DokusNavigationRailSectioned(
                    sections = NavDefinition.sections,
                    expandedSections = expandedSections,
                    selectedRoute = selectedRoute,
                    settingsItem = null,
                    onSectionToggle = { sectionId ->
                        // Accordion behavior: collapse all, expand clicked
                        val currentlyExpanded = expandedSections[sectionId] ?: false
                        if (!currentlyExpanded) {
                            expandedSections.keys.forEach { id ->
                                expandedSections[id] = (id == sectionId)
                            }
                        } else {
                            expandedSections[sectionId] = false
                        }
                    },
                    onItemClick = onNavItemClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                DesktopSidebarBottomControls(
                    tenantState = tenantState,
                    profileData = profileData,
                    isLoggingOut = isLoggingOut,
                    onWorkspaceClick = onWorkspaceClick,
                    onProfileClick = onProfileClick,
                    onAppearanceClick = onAppearanceClick,
                    onLogoutClick = onLogoutClick
                )
            }
        }

        // Main content area — detached glass container (clips app bars to rounded corners)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (topBarConfig != null) {
                    DesktopShellTopBar(topBarConfig = topBarConfig)
                }
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationLayout(
    selectedRoute: String?,
    topBarConfig: HomeShellTopBarConfig?,
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onTabClick: (MobileTabConfig) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (topBarConfig != null) {
                MobileShellTopBar(
                    topBarConfig = topBarConfig,
                    tenantState = tenantState,
                    profileData = profileData,
                    isLoggingOut = isLoggingOut,
                    onWorkspaceClick = onWorkspaceClick,
                    onProfileClick = onProfileClick,
                    onAppearanceClick = onAppearanceClick,
                    onLogoutClick = onLogoutClick
                )
            }
        },
        bottomBar = {
            // Calm, "Dokus" bottom shell: no tinted slab; keep accent only for the selected item.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                )
            ) {
                DokusNavigationBar(
                    tabs = NavDefinition.mobileTabs,
                    selectedRoute = selectedRoute,
                    onTabClick = onTabClick,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

private fun buildProfileData(
    user: User?,
    tierLabel: String?,
): HomeShellProfileData? {
    user ?: return null
    val fullName = listOfNotNull(user.firstName?.value, user.lastName?.value)
        .joinToString(" ")
        .ifBlank { user.email.value }
    return HomeShellProfileData(
        fullName = fullName,
        email = user.email.value,
        tierLabel = tierLabel
    )
}

@Composable
private fun rememberFallbackShellTopBarConfig(
    normalizedRoute: String?,
    fallbackSearchQuery: String,
    onFallbackSearchQueryChange: (String) -> Unit,
    isMobileSearchExpanded: Boolean,
    onExpandSearch: () -> Unit,
): HomeShellTopBarConfig? {
    val defaultConfig = NavDefinition.resolveShellTopBarDefault(normalizedRoute) ?: return null
    return when (defaultConfig.mode) {
        NavDefinition.ShellTopBarDefaultMode.Search -> {
            HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Search(
                    query = fallbackSearchQuery,
                    placeholder = stringResource(Res.string.search_placeholder),
                    onQueryChange = onFallbackSearchQueryChange,
                    onClear = { onFallbackSearchQueryChange("") },
                    isSearchExpanded = isMobileSearchExpanded,
                    onExpandSearch = onExpandSearch
                )
            )
        }

        NavDefinition.ShellTopBarDefaultMode.Title -> {
            val titleRes = NavDefinition.findByRoute(normalizedRoute)?.titleRes ?: Res.string.cashflow_title
            HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title(
                    title = stringResource(titleRes)
                )
            )
        }
    }
}
