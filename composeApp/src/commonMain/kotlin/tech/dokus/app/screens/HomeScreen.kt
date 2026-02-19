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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.app.allNavItems
import tech.dokus.app.homeNavigationProviders
import tech.dokus.app.mobileTabConfigs
import tech.dokus.app.navSectionsCombined
import tech.dokus.app.navigation.local.HomeNavControllerProvided
import tech.dokus.app.screens.documentdetail.DocumentDetailMode
import tech.dokus.app.screens.home.DesktopSidebarBottomControls
import tech.dokus.app.screens.home.DesktopShellTopBar
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.app.screens.home.MobileShellTopBar
import tech.dokus.app.screens.home.buildSortedRoutes
import tech.dokus.app.screens.home.normalizeRoute
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
import tech.dokus.foundation.app.shell.DocDetailModeHost
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarHost
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.LocalDocDetailModeHost
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode
import tech.dokus.foundation.app.shell.LocalHomeShellTopBarHost
import tech.dokus.domain.ids.DocumentId
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRailSectioned
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.destinations.route
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
    val navSections = remember(appModules) { appModules.navSectionsCombined }
    val mobileTabs = remember(appModules) { appModules.mobileTabConfigs }
    val allNavItems = remember(appModules) { appModules.allNavItems }
    val sortedRoutes = remember(allNavItems) { buildSortedRoutes(allNavItems) }
    val startDestination = remember(navSections) { navSections.first().items.first().destination }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val errorMessage = pendingError?.localized
    val isLargeScreen = LocalScreenSize.current.isLarge
    var fallbackSearchQuery by rememberSaveable { mutableStateOf("") }
    var isMobileSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }

    // Document detail mode state
    var docDetailDocuments by remember { mutableStateOf(emptyList<DocQueueItem>()) }
    var docDetailSelectedId by remember { mutableStateOf<DocumentId?>(null) }
    val isDocDetailMode = isLargeScreen && docDetailSelectedId != null

    val docDetailModeHost = remember(homeNavController) {
        object : DocDetailModeHost {
            override fun enter(documentId: DocumentId, documents: List<DocQueueItem>) {
                docDetailDocuments = documents
                docDetailSelectedId = documentId
                homeNavController.navigate(
                    CashFlowDestination.DocumentReview(documentId.toString())
                )
            }

            override fun select(documentId: DocumentId) {
                docDetailSelectedId = documentId
                homeNavController.navigate(
                    CashFlowDestination.DocumentReview(documentId.toString())
                ) {
                    // Replace current DocumentReview with new one
                    homeNavController.currentBackStackEntry?.destination?.route?.let { route ->
                        popUpTo(route) { inclusive = true }
                    }
                }
            }

            override fun exit() {
                docDetailDocuments = emptyList()
                docDetailSelectedId = null
                homeNavController.popBackStack()
            }
        }
    }
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
    val normalizedRoute = normalizeRoute(currentRoute, sortedRoutes)

    val shellState = state as? HomeState.Ready ?: HomeState.Ready()
    val tenant = (shellState.tenantState as? DokusState.Success<Tenant>)?.data
    val user = (shellState.userState as? DokusState.Success<User>)?.data
    val fallbackShellTopBarConfig = rememberFallbackShellTopBarConfig(
        normalizedRoute = normalizedRoute,
        allNavItems = allNavItems,
        fallbackSearchQuery = fallbackSearchQuery,
        onFallbackSearchQueryChange = { fallbackSearchQuery = it },
        isMobileSearchExpanded = isMobileSearchExpanded,
        onExpandSearch = { isMobileSearchExpanded = true }
    )
    val topBarConfig = resolveHomeShellTopBarConfig(
        route = currentRoute,
        allNavItems = allNavItems,
        sortedRoutes = sortedRoutes,
        registeredConfigs = registeredTopBarConfigs,
        fallback = { _, _ -> fallbackShellTopBarConfig }
    )
    val profileData = buildProfileData(
        user = user,
        tierLabel = tenant?.subscription?.localized
    )

    val navHostContent: @Composable () -> Unit = {
        HomeNavControllerProvided(homeNavController) {
            CompositionLocalProvider(
                LocalHomeShellTopBarHost provides topBarHost,
                LocalDocDetailModeHost provides docDetailModeHost,
            ) {
                HomeNavHost(
                    navHostController = homeNavController,
                    homeNavProviders = homeNavProviders,
                    startDestination = startDestination
                )
            }
        }
    }

    Surface {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isDocDetailMode) {
                DocumentDetailMode(
                    documents = docDetailDocuments,
                    selectedDocumentId = docDetailSelectedId!!,
                    onSelectDocument = { docDetailModeHost.select(it) },
                    onExit = { docDetailModeHost.exit() },
                    content = navHostContent,
                )
            } else if (isLargeScreen) {
                RailNavigationLayout(
                    navSections = navSections,
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
                        homeNavController.navigateTo(navItem.destination)
                    },
                    content = navHostContent,
                )
            } else {
                BottomNavigationLayout(
                    mobileTabs = mobileTabs,
                    selectedRoute = currentRoute,
                    profileData = profileData,
                    onProfileClick = { navController.navigateTo(AuthDestination.ProfileSettings) },
                    onTabClick = { tab ->
                        tab.destination?.let { destination ->
                            homeNavController.navigateTo(destination)
                        }
                    },
                    content = navHostContent,
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
    val isLargeScreen = LocalScreenSize.current.isLarge
    val transitionsProvider: TransitionsProvider = remember(isLargeScreen) {
        TransitionsProvider.forTabs(isLargeScreen)
    }
    NavHost(
        navHostController,
        startDestination = startDestination,
        enterTransition = { with(transitionsProvider) { enterTransition } },
        exitTransition = { with(transitionsProvider) { exitTransition } },
        popEnterTransition = { with(transitionsProvider) { popEnterTransition } },
        popExitTransition = { with(transitionsProvider) { popExitTransition } },
        modifier = Modifier.background(
            if (LocalIsInDocDetailMode.current) Color.Transparent
            else MaterialTheme.colorScheme.background
        ),
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
    navSections: List<NavSection>,
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
    val expandedSections = remember(navSections) {
        mutableStateMapOf<String, Boolean>().apply {
            navSections.forEach { section ->
                put(section.id, section.defaultExpanded)
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    // Floating glass windows desktop shell (v2)
    Box(
        Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()
        Row(Modifier.fillMaxSize().padding(Constrains.Shell.padding)) {
        // Sidebar glass panel
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(Constrains.Shell.sidebarWidth),
            shape = MaterialTheme.shapes.large,
            color = colorScheme.glass,
            border = BorderStroke(1.dp, colorScheme.glassBorder),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                AppNameText(modifier = Modifier.padding(bottom = 24.dp))

                DokusNavigationRailSectioned(
                    sections = navSections,
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

        // Content glass panel
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Constrains.Shell.gap),
            color = colorScheme.glassContent,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, colorScheme.glassBorder),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
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
    } // Row
    } // Box
}

/** Routes where the shell header (Dokus + avatar) is shown. Other tabs provide their own top bar. */
private val ShellHeaderRoutes = setOf("today", "documents", "cashflow", "more")

@Composable
private fun BottomNavigationLayout(
    mobileTabs: List<MobileTabConfig>,
    selectedRoute: String?,
    profileData: HomeShellProfileData?,
    onProfileClick: () -> Unit,
    onTabClick: (MobileTabConfig) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val showShellHeader = selectedRoute in ShellHeaderRoutes

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showShellHeader) {
                MobileShellTopBar(
                    profileData = profileData,
                    onProfileClick = onProfileClick,
                )
            }
        },
        bottomBar = {
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
                    tabs = mobileTabs,
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
    allNavItems: List<NavItem>,
    fallbackSearchQuery: String,
    onFallbackSearchQueryChange: (String) -> Unit,
    isMobileSearchExpanded: Boolean,
    onExpandSearch: () -> Unit,
): HomeShellTopBarConfig? {
    val navItem = allNavItems.find { it.destination.route == normalizedRoute } ?: return null
    val shellDefault = navItem.shellTopBar ?: return null
    return when (shellDefault) {
        ShellTopBarDefault.Search -> {
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

        ShellTopBarDefault.Title -> {
            HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title(
                    title = stringResource(navItem.titleRes)
                )
            )
        }
    }
}
