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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.navigation.HomeNavigationCommandBus
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.app.navigation.HomeNavigationSource
import tech.dokus.app.screens.home.DesktopShellTopBar
import tech.dokus.app.screens.home.DesktopSidebarBottomControls
import tech.dokus.app.screens.bc.BCHomeRoute
import tech.dokus.app.screens.home.CMHomeRoute
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.app.screens.home.MobileShellTopBar
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
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.UserAccessContext
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRailSectioned
import tech.dokus.foundation.aura.components.text.DokusLogo
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.model.ShellTopBarDefault
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController

private enum class HomeSurfaceMode {
    CM,
    BC,
}

@Composable
internal fun HomeRoute(
    appModules: List<AppModule> = LocalAppModules.current,
    container: HomeContainer = container(),
) {
    val navController = LocalNavController.current
    val pendingHomeCommand by HomeNavigationCommandBus.pendingCommand.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var activeSurface by rememberSaveable { mutableStateOf(HomeSurfaceMode.CM) }
    val errorMessage = pendingError?.localized
    val isLargeScreen = LocalScreenSize.current.isLarge

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is HomeAction.ShowError -> pendingError = action.error
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(HomeIntent.ScreenAppeared)
    }

    val shellState = state as? HomeState.Ready ?: HomeState.Ready()
    val surfaceAvailability = shellState.surfaceAvailability
    val tenant = (shellState.tenantState as? DokusState.Success<Tenant>)?.data
    val user = (shellState.userState as? DokusState.Success<User>)?.data
    val profileData = buildProfileData(
        user = user,
        tierLabel = tenant?.subscription?.localized
    )

    LaunchedEffect(surfaceAvailability?.canCompanyManager, surfaceAvailability?.canBookkeeperConsole) {
        if (surfaceAvailability?.canBookkeeperConsole == true && surfaceAvailability.canCompanyManager.not()) {
            activeSurface = HomeSurfaceMode.BC
        }
        if (surfaceAvailability?.canBookkeeperConsole == false && activeSurface == HomeSurfaceMode.BC) {
            activeSurface = HomeSurfaceMode.CM
        }
    }

    LaunchedEffect(
        pendingHomeCommand?.id,
        activeSurface,
        surfaceAvailability?.canCompanyManager,
        surfaceAvailability?.canBookkeeperConsole,
    ) {
        val pending = pendingHomeCommand ?: return@LaunchedEffect
        activeSurface = resolveSurfaceForCommand(
            command = pending.command,
            currentSurface = activeSurface,
            surfaceAvailability = surfaceAvailability,
        )
    }

    val showBC = shouldRenderBC(
        activeSurface = activeSurface,
        surfaceAvailability = surfaceAvailability
    )

    val onConsumeHomeCommand = remember { { id: Long -> HomeNavigationCommandBus.consume(id) } }
    val onSwitchToCM = remember { { activeSurface = HomeSurfaceMode.CM } }
    val onSwitchToBC = remember { { activeSurface = HomeSurfaceMode.BC } }
    val onLogoutClick = remember(container) { { container.store.intent(HomeIntent.Logout) } }

    if (showBC) {
        BCHomeRoute(
            appModules = appModules,
            rootNavController = navController,
            isLargeScreen = isLargeScreen,
            shellState = shellState,
            tenant = tenant,
            profileData = profileData,
            pendingHomeCommand = pendingHomeCommand,
            onConsumeHomeCommand = onConsumeHomeCommand,
            onSwitchToCM = onSwitchToCM,
            onLogoutClick = onLogoutClick,
            snackbarHostState = snackbarHostState,
        )
    } else {
        CMHomeRoute(
            appModules = appModules,
            rootNavController = navController,
            isLargeScreen = isLargeScreen,
            shellState = shellState,
            tenant = tenant,
            profileData = profileData,
            pendingHomeCommand = pendingHomeCommand,
            onConsumeHomeCommand = onConsumeHomeCommand,
            onSwitchToBC = onSwitchToBC,
            onLogoutClick = onLogoutClick,
            snackbarHostState = snackbarHostState,
        )
    }
}

private fun resolveSurfaceForCommand(
    command: HomeNavigationCommand,
    currentSurface: HomeSurfaceMode,
    surfaceAvailability: tech.dokus.domain.model.auth.SurfaceAvailability?,
): HomeSurfaceMode {
    val resolved = surfaceAvailability != null
    val canCM = surfaceAvailability?.canCompanyManager ?: true
    val canBC = surfaceAvailability?.canBookkeeperConsole == true
    val canBCAccess = !resolved || canBC
    val isBCOnly = resolved && canBC && !canCM

    if (isBCOnly) return HomeSurfaceMode.BC

    return when (command) {
        HomeNavigationCommand.OpenConsoleClients -> {
            if (canBCAccess) HomeSurfaceMode.BC else HomeSurfaceMode.CM
        }

        is HomeNavigationCommand.OpenDocuments -> {
            if (command.source == HomeNavigationSource.BC && canBCAccess) {
                HomeSurfaceMode.BC
            } else {
                HomeSurfaceMode.CM
            }
        }

        is HomeNavigationCommand.OpenDocumentReview -> {
            if (currentSurface == HomeSurfaceMode.BC && canBCAccess) {
                HomeSurfaceMode.BC
            } else {
                HomeSurfaceMode.CM
            }
        }
    }
}

private fun shouldRenderBC(
    activeSurface: HomeSurfaceMode,
    surfaceAvailability: tech.dokus.domain.model.auth.SurfaceAvailability?,
): Boolean {
    if (surfaceAvailability == null) {
        return activeSurface == HomeSurfaceMode.BC
    }
    if (!surfaceAvailability.canBookkeeperConsole) return false
    if (!surfaceAvailability.canCompanyManager) return true
    return activeSurface == HomeSurfaceMode.BC
}

@Composable
internal fun HomeScreen(
    navSections: List<NavSection>,
    mobileTabs: List<MobileTabConfig>,
    selectedRoute: String?,
    topBarConfig: HomeShellTopBarConfig?,
    desktopPinnedItems: List<NavItem>,
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    snackbarHostState: SnackbarHostState,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavItemClick: (NavItem) -> Unit,
    onTabClick: (MobileTabConfig) -> Unit,
    onSearchShortcut: () -> Unit,
    content: @Composable () -> Unit,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Surface(
        modifier = Modifier.onPreviewKeyEvent { keyEvent ->
            if (
                keyEvent.type == KeyEventType.KeyDown &&
                keyEvent.key == Key.K &&
                (keyEvent.isMetaPressed || keyEvent.isCtrlPressed)
            ) {
                onSearchShortcut()
                true
            } else {
                false
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                RailNavigationLayout(
                    navSections = navSections,
                    pinnedItems = desktopPinnedItems,
                    selectedRoute = selectedRoute,
                    topBarConfig = topBarConfig,
                    tenantState = tenantState,
                    profileData = profileData,
                    isLoggingOut = isLoggingOut,
                    onWorkspaceClick = onWorkspaceClick,
                    onProfileClick = onProfileClick,
                    onAppearanceClick = onAppearanceClick,
                    onLogoutClick = onLogoutClick,
                    onNavItemClick = onNavItemClick,
                    content = content,
                )
            } else {
                BottomNavigationLayout(
                    mobileTabs = mobileTabs,
                    selectedRoute = selectedRoute,
                    profileData = profileData,
                    onProfileClick = onProfileClick,
                    onTabClick = onTabClick,
                    content = content,
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
internal fun HomeNavHost(
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
    navSections: List<NavSection>,
    pinnedItems: List<NavItem>,
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
    val expandedSections = remember(navSections) {
        mutableStateMapOf<String, Boolean>().apply {
            navSections.forEach { section ->
                put(section.id, section.defaultExpanded)
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()
        Row(Modifier.fillMaxSize().padding(Constraints.Shell.padding)) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(Constraints.Shell.sidebarWidth),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.glass,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    DokusLogo.Full(modifier = Modifier.padding(bottom = 28.dp))

                    DokusNavigationRailSectioned(
                        sections = navSections,
                        pinnedItems = pinnedItems,
                        expandedSections = expandedSections,
                        selectedRoute = selectedRoute,
                        settingsItem = null,
                        onSectionToggle = { sectionId ->
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

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Constraints.Shell.gap),
                color = colorScheme.glassContent,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp,
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
private val ShellHeaderRoutes = setOf("today", "documents", "search", "cashflow", "more")

internal fun filterCMNavItems(
    items: List<NavItem>,
    accessContext: UserAccessContext,
): List<NavItem> {
    return items.filter { item ->
        when {
            item.destination == HomeDestination.Accountant && !accessContext.canBookkeeperConsole -> false
            accessContext.isBookkeeperConsoleOnly -> item.destination == HomeDestination.Accountant ||
                item.destination == HomeDestination.Documents
            accessContext.isStage2ReadOnly && item.destination == HomeDestination.Cashflow -> false
            else -> true
        }
    }
}

internal fun dispatchProfileNavigation(
    isLargeScreen: Boolean,
    onNavigateHomeProfile: () -> Unit,
    onNavigateRootProfile: () -> Unit,
) {
    if (isLargeScreen) {
        onNavigateHomeProfile()
    } else {
        onNavigateRootProfile()
    }
}

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
internal fun rememberFallbackShellTopBarConfig(
    normalizedRoute: String?,
    allNavItems: List<NavItem>,
): HomeShellTopBarConfig? {
    val navItem = allNavItems.find { it.destination.route == normalizedRoute } ?: return null
    val shellDefault = navItem.shellTopBar ?: return null
    return when (shellDefault) {
        ShellTopBarDefault.Title -> {
            val subtitle = navItem.subtitleRes?.let { stringResource(it) }
            HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title(
                    title = stringResource(navItem.titleRes),
                    subtitle = subtitle
                )
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        HomeScreen(
            navSections = emptyList(),
            mobileTabs = emptyList(),
            selectedRoute = null,
            topBarConfig = null,
            desktopPinnedItems = emptyList(),
            tenantState = DokusState.loading(),
            profileData = null,
            isLoggingOut = false,
            snackbarHostState = remember { SnackbarHostState() },
            onWorkspaceClick = {},
            onProfileClick = {},
            onAppearanceClick = {},
            onLogoutClick = {},
            onNavItemClick = {},
            onTabClick = {},
            onSearchShortcut = {},
            content = {},
        )
    }
}
