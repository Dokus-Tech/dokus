package tech.dokus.app.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import tech.dokus.app.homeItems
import tech.dokus.app.homeNavigationProviders
import tech.dokus.app.navigation.NavDefinition
import tech.dokus.app.viewmodel.HomeContainer
import tech.dokus.app.viewmodel.HomeIntent
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRailSectioned
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
import tech.dokus.navigation.local.NavControllerProvided
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.rememberSelectedDestination

/**
 * Home screen using FlowMVI Container pattern.
 * Main navigation shell containing bottom navigation (mobile) or navigation rail (desktop).
 */
@Composable
internal fun HomeScreen(
    appModules: List<AppModule> = LocalAppModules.current,
    container: HomeContainer = container(),
) {
    val homeNavProviders = remember(appModules) { appModules.homeNavigationProviders }
    val homeItems = remember(appModules) { appModules.homeItems }

    val homeNavController = rememberNavController()

    val homeDestinations = remember(homeItems) { homeItems.map { it.destination } }
    val currentDestination = rememberSelectedDestination(homeNavController, homeDestinations)

    val selectedItem = remember(currentDestination) {
        homeItems.find { it.destination == currentDestination }
    } ?: homeItems.first()

    // Subscribe to store (no actions to handle for this navigation shell)
    container.store.subscribe(DefaultLifecycle) { _ ->
        // No actions to handle
    }

    // Notify container when screen appears
    LaunchedEffect(Unit) {
        container.store.intent(HomeIntent.ScreenAppeared)
    }

    // Get current route directly from backstack
    val navBackStackEntry by homeNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface {
        if (LocalScreenSize.isLarge) {
            RailNavigationLayout(
                selectedRoute = currentRoute,
                onNavItemClick = { navItem ->
                    NavDefinition.routeToDestination(navItem.route)?.let { destination ->
                        homeNavController.navigateTo(destination)
                    }
                },
                content = {
                    HomeNavHost(
                        navHostController = homeNavController,
                        homeNavProviders = homeNavProviders,
                        selectedItem = selectedItem
                    )
                }
            )
        } else {
            BottomNavigationLayout(
                selectedRoute = currentRoute,
                onTabClick = { tab ->
                    tab.route?.let { route ->
                        NavDefinition.routeToDestination(route)?.let { destination ->
                            homeNavController.navigateTo(destination)
                        }
                    }
                },
                content = {
                    HomeNavHost(
                        navHostController = homeNavController,
                        homeNavProviders = homeNavProviders,
                        selectedItem = selectedItem
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeNavHost(
    navHostController: NavHostController,
    homeNavProviders: List<NavigationProvider>,
    selectedItem: HomeItem,
) {
    val transitionsProvider: TransitionsProvider = remember { TransitionsProvider.forTabs() }
    // Provide home nav controller for screens that need to navigate within home
    NavControllerProvided(navHostController) {
        NavHost(
            navHostController,
            startDestination = selectedItem.destination,
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
}

@Composable
private fun RailNavigationLayout(
    selectedRoute: String?,
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
                    settingsItem = NavDefinition.Items.settings,
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavigationLayout(
    selectedRoute: String?,
    onTabClick: (MobileTabConfig) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // Look up if current route should show top bar
    val currentNavItem = NavDefinition.findByRoute(selectedRoute)
    val showTopBar = currentNavItem?.showTopBar ?: false
    val titleRes = currentNavItem?.titleRes

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showTopBar && titleRes != null) {
                PTopAppBar(
                    title = stringResource(titleRes)
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
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
