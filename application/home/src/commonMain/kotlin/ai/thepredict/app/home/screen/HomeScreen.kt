package ai.thepredict.app.home.screen

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.ui.navigation.NavigationBar
import ai.thepredict.ui.navigation.TabNavItem
import ai.thepredict.ui.navigation.findByScreenKey
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator

private val TabNavItem.Companion.items: List<TabNavItem>
    @Composable get() = if (isLargeScreen) {
        listOf(
            TabNavItem.Dashboard,
            TabNavItem.Contacts,
            TabNavItem.Cashflow,
            TabNavItem.Simulations,
            TabNavItem.Inventory,
            TabNavItem.Banking,
            TabNavItem.Profile
        )
    } else {
        listOf(
            TabNavItem.Dashboard,
            TabNavItem.Contacts,
            TabNavItem.Inventory,
            TabNavItem.Banking
        )
    }

internal class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navItems = TabNavItem.items

        val dashboardTab = rememberScreen(TabNavItem.Dashboard.screenProvider)

        Scaffold {
            Navigator(dashboardTab) { navigator ->
                val onNavItemSelected: (TabNavItem) -> Unit = { item ->
                    when (item) {
                        TabNavItem.Dashboard -> navigator.replaceAll(dashboardTab)
                        else -> {}
                    }
                }

                val selectedItem = navItems.findByScreenKey(navigator.lastItem.key)

                val layout: @Composable (@Composable () -> Unit) -> Unit =
                    if (isLargeScreen) {
                        { content ->
                            RailNavigationLayout(
                                selectedItem,
                                navItems,
                                onNavItemSelected
                            ) { content() }
                        }
                    } else {
                        { content ->
                            BottomNavigationLayout(
                                selectedItem,
                                navItems,
                                onNavItemSelected
                            ) { content() }
                        }
                    }

                layout {
                    CurrentScreen()
                }
            }
        }
    }
}

@Composable
private fun RailNavigationLayout(
    selectedItem: TabNavItem,
    navItems: List<TabNavItem>,
    onSelectedItemChange: (TabNavItem) -> Unit,
    content: @Composable () -> Unit
) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail {
            val mainItems = navItems.dropLast(1)
            val settingsItem = navItems.last()

            mainItems.forEach { item ->
                NavigationRailItem(
                    selected = selectedItem == item,
                    onClick = { onSelectedItemChange(item) },
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) }
                )
            }

            Spacer(Modifier.weight(1f, fill = true))

            NavigationRailItem(
                selected = selectedItem == settingsItem,
                onClick = { onSelectedItemChange(settingsItem) },
                icon = { Icon(settingsItem.icon, contentDescription = settingsItem.title) },
                label = { Text(settingsItem.title) }
            )
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
    selectedItem: TabNavItem,
    navItems: List<TabNavItem>,
    onSelectedItemChange: (TabNavItem) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (selectedItem.showTopBar) {
                CenterAlignedTopAppBar(
                    title = { Text(selectedItem.title) }
                )
            }
        },
        bottomBar = {
            NavigationBar(
                tabNavItems = navItems,
                fabItem = TabNavItem.Fab.AddDocuments,
                selectedIndex = navItems.indexOf(selectedItem),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                onSelectedItemChange(it)
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