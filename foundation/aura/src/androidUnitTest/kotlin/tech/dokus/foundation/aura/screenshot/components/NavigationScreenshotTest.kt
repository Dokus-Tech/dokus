package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.resources.painterResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.home_settings
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_more
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.nav_team
import tech.dokus.aura.resources.settings
import tech.dokus.aura.resources.users
import tech.dokus.foundation.aura.components.navigation.DokusNavigationBar
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRail
import tech.dokus.foundation.aura.components.navigation.DokusNavigationRailSectioned
import tech.dokus.foundation.aura.components.navigation.SelectableCard
import tech.dokus.foundation.aura.components.navigation.TabNavItem
import tech.dokus.foundation.aura.components.navigation.navItems
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport
import tech.dokus.navigation.local.NavControllerProvided
import tech.dokus.foundation.aura.components.navigation.NavigationBar as LegacyNavigationBar

/**
 * Screenshot tests for navigation components.
 * Note: DokusNavigationBar requires HomeItem or MobileTabConfig with resources,
 * so we test the navigation pattern directly using Material3 NavigationBar.
 */
@RunWith(Parameterized::class)
class NavigationScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun navigationBar_threeItems() {
        paparazzi.snapshotAllViewports("NavigationBar_threeItems", viewport) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Contacts") },
                    label = { Text("Contacts") },
                    selected = false,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }

    @Test
    fun navigationBar_middleSelected() {
        paparazzi.snapshotAllViewports("NavigationBar_middleSelected", viewport) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Contacts") },
                    label = { Text("Contacts") },
                    selected = true,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, "Settings") },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {},
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    }

    @Test
    fun navigationBar_fourItems() {
        paparazzi.snapshotAllViewports("NavigationBar_fourItems", viewport) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                listOf(
                    Triple(Icons.Default.Home, "Home", true),
                    Triple(Icons.Default.Person, "Contacts", false),
                    Triple(Icons.Default.Settings, "Settings", false),
                    Triple(Icons.Default.Person, "Profile", false)
                ).forEach { (icon, label, selected) ->
                    NavigationBarItem(
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        selected = selected,
                        onClick = {},
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }

    @Test
    fun dokusNavigationBar_homeItems() {
        val items = sampleHomeItems()
        paparazzi.snapshotAllViewports("DokusNavigationBar_homeItems", viewport) {
            DokusNavigationBar(
                navItems = items,
                selectedItem = items.first(),
                onSelectedItemChange = {}
            )
        }
    }

    @Test
    fun dokusNavigationBar_tabs() {
        val tabs = listOf(
            MobileTabConfig("today", Res.string.home_today, Res.drawable.chart_bar_trend_up, "tab/today"),
            MobileTabConfig("contacts", Res.string.nav_contacts, Res.drawable.users, "tab/contacts"),
            MobileTabConfig("cashflow", Res.string.cashflow_title, Res.drawable.cashflow, "tab/cashflow"),
            MobileTabConfig("more", Res.string.nav_more, Res.drawable.more_horizontal, null)
        )
        paparazzi.snapshotAllViewports("DokusNavigationBar_tabs", viewport) {
            DokusNavigationBar(
                tabs = tabs,
                selectedRoute = "tab/today",
                onTabClick = {}
            )
        }
    }

    @Test
    fun dokusNavigationRail_homeItems() {
        val items = sampleHomeItems()
        paparazzi.snapshotAllViewports("DokusNavigationRail_homeItems", viewport) {
            androidx.compose.foundation.layout.Column {
                DokusNavigationRail(
                    selectedItem = items.first(),
                    navItems = items,
                    onSelectedItemChange = {}
                )
            }
        }
    }

    @Test
    fun dokusNavigationRailSectioned() {
        val sections = listOf(
            NavSection(
                id = "accounting",
                titleRes = Res.string.nav_section_accounting,
                iconRes = Res.drawable.chart_bar_trend_up,
                items = listOf(
                    NavItem("documents", Res.string.nav_documents, Res.drawable.file_text, "documents"),
                    NavItem("cashflow", Res.string.cashflow_title, Res.drawable.cashflow, "cashflow")
                ),
                defaultExpanded = true
            ),
            NavSection(
                id = "company",
                titleRes = Res.string.nav_section_company,
                iconRes = Res.drawable.users,
                items = listOf(
                    NavItem("contacts", Res.string.nav_contacts, Res.drawable.users, "contacts"),
                    NavItem("team", Res.string.nav_team, Res.drawable.users, "team", comingSoon = true)
                ),
                defaultExpanded = false
            )
        )
        val expanded = mapOf("accounting" to true, "company" to false)
        paparazzi.snapshotAllViewports("DokusNavigationRailSectioned", viewport) {
            androidx.compose.foundation.layout.Column {
                DokusNavigationRailSectioned(
                    sections = sections,
                    expandedSections = expanded,
                    selectedRoute = "documents",
                    settingsItem = NavItem("settings", Res.string.home_settings, Res.drawable.settings, "settings"),
                    onSectionToggle = {},
                    onItemClick = {}
                )
            }
        }
    }

    @Test
    fun legacyNavigationBar_withFab() {
        paparazzi.snapshotAllViewports("NavigationBar_withFab", viewport) {
            LegacyNavigationBar(
                items = navItems,
                fabItem = TabNavItem.AddDocuments,
                selectedIndex = 2,
                onItemClick = {},
                onFabClick = {}
            )
        }
    }

    @Test
    fun selectableCard_states() {
        paparazzi.snapshotAllViewports("SelectableCard_states", viewport) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                SelectableCard(
                    title = "Documents",
                    icon = painterResource(Res.drawable.file_text),
                    isSelected = true,
                    onClick = {}
                )
                SelectableCard(
                    title = "Contacts",
                    icon = painterResource(Res.drawable.users),
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun userPreferencesMenu_closed() {
        paparazzi.snapshotAllViewports("UserPreferencesMenu_closed", viewport) {
            val navController = rememberNavController()
            NavControllerProvided(navController) {
                UserPreferencesMenuPreview()
            }
        }
    }
}

@Composable
private fun UserPreferencesMenuPreview() {
    tech.dokus.foundation.aura.components.navigation.UserPreferencesMenu()
}

private fun sampleHomeItems(): List<HomeItem> = listOf(
    HomeItem(
        titleRes = Res.string.home_today,
        iconRes = Res.drawable.chart_bar_trend_up,
        destination = tech.dokus.navigation.destinations.HomeDestination.Today,
        showTopBar = false
    ),
    HomeItem(
        titleRes = Res.string.nav_documents,
        iconRes = Res.drawable.file_text,
        destination = tech.dokus.navigation.destinations.HomeDestination.Documents,
        showTopBar = true
    ),
    HomeItem(
        titleRes = Res.string.nav_contacts,
        iconRes = Res.drawable.users,
        destination = tech.dokus.navigation.destinations.HomeDestination.Contacts,
        showTopBar = true
    ),
    HomeItem(
        titleRes = Res.string.home_settings,
        iconRes = Res.drawable.settings,
        destination = tech.dokus.navigation.destinations.HomeDestination.Settings,
        showTopBar = true
    )
)
