package tech.dokus.foundation.aura.screenshot.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotBothThemes
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for navigation components.
 * Note: DokusNavigationBar requires HomeItem or MobileTabConfig with resources,
 * so we test the navigation pattern directly using Material3 NavigationBar.
 */
class NavigationScreenshotTest {

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun navigationBar_threeItems() {
        paparazzi.snapshotBothThemes("NavigationBar_threeItems") {
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
        paparazzi.snapshotBothThemes("NavigationBar_middleSelected") {
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
        paparazzi.snapshotBothThemes("NavigationBar_fourItems") {
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
}
