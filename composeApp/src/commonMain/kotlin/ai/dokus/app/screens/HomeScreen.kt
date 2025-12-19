package ai.dokus.app.screens

import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules
import ai.dokus.app.homeItems
import ai.dokus.app.homeNavigationProviders
import ai.dokus.app.viewmodel.HomeViewModel
import ai.dokus.foundation.design.components.navigation.DokusNavigationBar
import ai.dokus.foundation.design.components.navigation.DokusNavigationRail
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.animation.TransitionsProvider
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.rememberSelectedDestination
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    appModules: List<AppModule> = LocalAppModules.current,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val homeNavProviders = remember(appModules) { appModules.homeNavigationProviders }
    val homeItems = remember(appModules) { appModules.homeItems }

    val homeNavController = rememberNavController()

    val homeDestinations = remember(homeItems) { homeItems.map { it.destination } }
    val currentDestination = rememberSelectedDestination(homeNavController, homeDestinations)

    val selectedItem = remember(currentDestination) {
        homeItems.find { it.destination == currentDestination }
    } ?: homeItems.first()

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
    Row(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(240.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                AppNameText(modifier = Modifier.padding(bottom = 32.dp))

                DokusNavigationRail(
                    selectedItem = selectedItem,
                    navItems = navItems,
                    onSelectedItemChange = onSelectedItemChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 8.dp),
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
                ai.dokus.foundation.design.components.common.PTopAppBar(
                    title = org.jetbrains.compose.resources.stringResource(selectedItem.titleRes)
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