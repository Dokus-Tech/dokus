package ai.dokus.app.app.home.screen

import ai.dokus.app.app.core.constrains.isLargeScreen
import ai.dokus.app.app.navigation.AppNavigator
import ai.dokus.app.app.navigation.AppRoutes
import ai.dokus.foundation.ui.navigation.NavigationBar
import ai.dokus.foundation.ui.navigation.NavigationRail
import ai.dokus.foundation.ui.navigation.TabNavItem
import ai.dokus.foundation.ui.navigation.navItems
import ai.dokus.foundation.ui.text.AppNameText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.dokus.app.app.banking.screen.BankingScreen
import ai.dokus.app.app.cashflow.screen.CashflowScreen
import ai.dokus.app.app.contacts.screen.ContactsScreen
import ai.dokus.app.app.dashboard.screen.DashboardScreen
import ai.dokus.app.app.inventory.screen.InventoryScreen
import ai.dokus.app.app.profile.screen.ProfileScreen
import ai.dokus.app.app.simulations.screen.SimulationScreen

@Composable
fun HomeScreen(
    navigator: AppNavigator
) {
    val tabNavController = rememberNavController()
    var currentRoute by remember { mutableStateOf(AppRoutes.TAB_DASHBOARD) }

    val tabItems = if (isLargeScreen) {
        navItems
    } else {
        listOf(
            TabNavItem.Dashboard,
            TabNavItem.Contacts,
            TabNavItem.Cashflow,
            TabNavItem.Banking
        )
    }

    val selectedIndex = tabItems.indexOfFirst { it.route == currentRoute }

    val onNavItemSelected: (TabNavItem) -> Unit = { item ->
        currentRoute = item.route
        tabNavController.navigate(item.route) {
            popUpTo(AppRoutes.TAB_DASHBOARD)
            launchSingleTop = true
        }
    }

    if (isLargeScreen) {
        HomeDesktopContent(
            tabNavController = tabNavController,
            navigator = navigator,
            selectedIndex = selectedIndex,
            tabItems = tabItems,
            onNavItemSelected = onNavItemSelected
        )
    } else {
        HomeMobileContent(
            tabNavController = tabNavController,
            navigator = navigator,
            selectedIndex = selectedIndex,
            tabItems = tabItems,
            onNavItemSelected = onNavItemSelected
        )
    }
}

@Composable
private fun HomeTabNavHost(
    navController: NavHostController,
    navigator: AppNavigator
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.TAB_DASHBOARD
    ) {
        composable(AppRoutes.TAB_DASHBOARD) {
            DashboardScreen(navigator)
        }

        composable(AppRoutes.TAB_CONTACTS) {
            ContactsScreen(navigator)
        }

        composable(AppRoutes.TAB_CASHFLOW) {
            CashflowScreen(navigator)
        }

        composable(AppRoutes.TAB_SIMULATIONS) {
            SimulationScreen(navigator)
        }

        composable(AppRoutes.TAB_INVENTORY) {
            InventoryScreen(navigator)
        }

        composable(AppRoutes.TAB_BANKING) {
            BankingScreen(navigator)
        }

        composable(AppRoutes.TAB_PROFILE) {
            ProfileScreen(navigator)
        }
    }
}

@Composable
private fun HomeDesktopContent(
    tabNavController: NavHostController,
    navigator: AppNavigator,
    selectedIndex: Int,
    tabItems: List<TabNavItem>,
    onNavItemSelected: (TabNavItem) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .width(250.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                NavigationRail(
                    selectedItem = tabItems[selectedIndex.coerceIn(0, tabItems.size - 1)],
                    navItems = tabItems,
                    onSelectedItemChange = onNavItemSelected,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { HomeTopBar() }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HomeTabNavHost(
                    navController = tabNavController,
                    navigator = navigator
                )
            }
        }
    }
}

@Composable
private fun HomeMobileContent(
    tabNavController: NavHostController,
    navigator: AppNavigator,
    selectedIndex: Int,
    tabItems: List<TabNavItem>,
    onNavItemSelected: (TabNavItem) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { HomeTopBar() },
        bottomBar = {
            NavigationBar(
                items = tabItems,
                fabItem = TabNavItem.AddDocuments,
                selectedIndex = selectedIndex,
                onItemClick = onNavItemSelected,
                onFabClick = {
                    // Handle fab click - navigate to add documents
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HomeTabNavHost(
                navController = tabNavController,
                navigator = navigator
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppNameText()
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}