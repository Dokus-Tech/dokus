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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.homeItems
import tech.dokus.app.homeNavigationProviders
import tech.dokus.app.viewmodel.HomeContainer
import tech.dokus.app.viewmodel.HomeIntent
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRail
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.animation.TransitionsProvider
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
    val state by container.store.subscribe(DefaultLifecycle) { _ ->
        // No actions to handle
    }

    // Notify container when screen appears
    LaunchedEffect(Unit) {
        container.store.intent(HomeIntent.ScreenAppeared)
    }

    Surface {
        if (LocalScreenSize.isLarge) {
            RailNavigationLayout(
                selectedItem = selectedItem,
                navItems = homeItems,
                onSelectedItemChange = { homeNavController.navigateTo(it.destination) },
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
                selectedItem = selectedItem,
                navItems = homeItems,
                onSelectedItemChange = { homeNavController.navigateTo(it.destination) },
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
    NavHost(
        navHostController,
        startDestination = selectedItem.destination,
        enterTransition = { with(transitionsProvider) { enterTransition } },
        exitTransition = { with(transitionsProvider) { exitTransition } },
        popEnterTransition = { with(transitionsProvider) { popEnterTransition } },
        popExitTransition = { with(transitionsProvider) { popExitTransition } },
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
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
    selectedItem: HomeItem,
    navItems: List<HomeItem>,
    onSelectedItemChange: (HomeItem) -> Unit,
    content: @Composable () -> Unit
) {
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
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                AppNameText(modifier = Modifier.padding(bottom = 24.dp))

                DokusNavigationRail(
                    selectedItem = selectedItem,
                    navItems = navItems,
                    onSelectedItemChange = onSelectedItemChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Main content area
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavigationLayout(
    selectedItem: HomeItem,
    navItems: List<HomeItem>,
    onSelectedItemChange: (HomeItem) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectedItem.showTopBar) {
                PTopAppBar(
                    title = stringResource(selectedItem.titleRes)
                )
            }
        },
        bottomBar = {
            DokusNavigationBar(
                navItems = navItems,
                selectedItem = selectedItem,
                onSelectedItemChange = onSelectedItemChange,
                modifier = Modifier.fillMaxWidth()
            )
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
