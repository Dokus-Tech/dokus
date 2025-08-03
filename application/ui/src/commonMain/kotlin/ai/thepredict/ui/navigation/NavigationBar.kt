package ai.thepredict.ui.navigation

import ai.thepredict.app.navigation.HomeTabsNavigation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import thepredict.application.ui.generated.resources.Res
import thepredict.application.ui.generated.resources.chart_bar_trend_up
import thepredict.application.ui.generated.resources.plus
import thepredict.application.ui.generated.resources.tasks_2
import thepredict.application.ui.generated.resources.users
import thepredict.application.ui.generated.resources.wallet_2


sealed interface NavigationItem {
    val icon: DrawableResource

    @get:Composable
    val label: String
    val route: String
    val screenProvider: HomeTabsNavigation

    data object Charts : NavigationItem {
        override val icon: DrawableResource = Res.drawable.chart_bar_trend_up
        override val label: String @Composable get() = "Dashboard"
        override val route: String = "dashboard"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Dashboard
    }

    data object Contacts : NavigationItem {
        override val icon: DrawableResource = Res.drawable.users
        override val label: String @Composable get() = "Contacts"
        override val route: String = "contacts"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Contacts
    }

    data object Inventory : NavigationItem {
        override val icon: DrawableResource = Res.drawable.tasks_2
        override val label: String @Composable get() = "Items"
        override val route: String = "items"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Items
    }

    data object Banks : NavigationItem {
        override val icon: DrawableResource = Res.drawable.wallet_2
        override val label: String @Composable get() = "Banking"
        override val route: String = "banking"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Banking
    }

    data object AddDocuments : NavigationItem {
        override val icon: DrawableResource = Res.drawable.plus
        override val label: String @Composable get() = "Add"
        override val route: String = "documents/add"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.AddDocuments
    }

    companion object {
        val all = listOf(
            Charts,
            Contacts,
            Inventory,
            Banks
        )
    }
}

@Composable
fun NavigationBar(
    navigationItems: List<NavigationItem>,
    fabItem: NavigationItem,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onItemClick: (NavigationItem) -> Unit,
) {
    val selectedItem = navigationItems[selectedIndex]
    val half = navigationItems.size / 2
    val firstHalf = navigationItems.take(half)
    val secondHalf = navigationItems.drop(half)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box(
            Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            firstHalf.forEach { item ->
                NavigationButton(
                    navigationItem = item,
                    onClick = { onItemClick(item) },
                    selected = selectedItem == item
                )
            }

            // Add button (FAB)
            FloatingActionButton(
                onClick = { onItemClick(fabItem) },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(fabItem.icon),
                    contentDescription = fabItem.label,
                    modifier = Modifier.size(20.dp)
                )
            }

            secondHalf.forEach { item ->
                NavigationButton(
                    navigationItem = item,
                    onClick = { onItemClick(item) },
                    selected = selectedItem == item
                )
            }
        }
    }
}

@Composable
private fun NavigationButton(
    navigationItem: NavigationItem,
    onClick: () -> Unit,
    selected: Boolean
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(navigationItem.icon),
            contentDescription = navigationItem.label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}