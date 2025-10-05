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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import thepredict.foundation.ui.generated.resources.Res
import thepredict.foundation.ui.generated.resources.cashflow
import thepredict.foundation.ui.generated.resources.chart_bar_trend_up
import thepredict.foundation.ui.generated.resources.plus
import thepredict.foundation.ui.generated.resources.simulations
import thepredict.foundation.ui.generated.resources.tasks_2
import thepredict.foundation.ui.generated.resources.user
import thepredict.foundation.ui.generated.resources.users
import thepredict.foundation.ui.generated.resources.wallet_2


sealed interface TabNavItem {
    @get:Composable
    val icon: Painter

    @get:Composable
    val title: String
    val route: String
    val screenProvider: HomeTabsNavigation
    val showTopBar: Boolean

    data object Dashboard : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.chart_bar_trend_up)
        override val title: String @Composable get() = "Dashboard"
        override val route: String = "dashboard"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Dashboard
        override val showTopBar: Boolean = false
    }

    data object Contacts : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.users)
        override val title: String @Composable get() = "Contacts"
        override val route: String = "contacts"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Contacts
        override val showTopBar: Boolean = true
    }

    data object Cashflow : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.cashflow)
        override val title: String @Composable get() = "Cashflow"
        override val route: String = "cashflow"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Cashflow
        override val showTopBar: Boolean = true
    }

    data object Simulations : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.simulations)
        override val title: String @Composable get() = "Simulations"
        override val route: String = "simulations"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Simulations
        override val showTopBar: Boolean = true
    }

    data object Inventory : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.tasks_2)
        override val title: String @Composable get() = "Items"
        override val route: String = "items"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Items
        override val showTopBar: Boolean = true
    }

    data object Banking : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.wallet_2)
        override val title: String @Composable get() = "Banking"
        override val route: String = "banking"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Banking
        override val showTopBar: Boolean = true
    }

    data object Profile : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.user)
        override val title: String @Composable get() = "Profile"
        override val route: String = "profile"
        override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.Profile
        override val showTopBar: Boolean = false
    }

    sealed interface Fab : TabNavItem {
        data object AddDocuments : Fab {
            override val icon: Painter @Composable get() = painterResource(Res.drawable.plus)
            override val title: String @Composable get() = "Add"
            override val route: String = "documents/add"
            override val screenProvider: HomeTabsNavigation = HomeTabsNavigation.AddDocuments
            override val showTopBar: Boolean = true
        }
    }

    companion object Companion {
        val all = listOf(
            Dashboard,
            Contacts,
            Cashflow,
            Simulations,
            Inventory,
            Banking,
            Profile
        )
    }
}

fun List<TabNavItem>.findByScreenKey(
    screenKey: String,
    default: TabNavItem = first()
): TabNavItem {
    return find { it.screenProvider.screenKey == screenKey } ?: default
}

@Composable
fun NavigationBar(
    tabNavItems: List<TabNavItem>,
    fabItem: TabNavItem,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onItemClick: (TabNavItem) -> Unit,
) {
    val selectedItem = tabNavItems[selectedIndex]
    val half = tabNavItems.size / 2
    val firstHalf = tabNavItems.take(half)
    val secondHalf = tabNavItems.drop(half)

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
                    tabNavItem = item,
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
                    painter = fabItem.icon,
                    contentDescription = fabItem.title,
                    modifier = Modifier.size(20.dp)
                )
            }

            secondHalf.forEach { item ->
                NavigationButton(
                    tabNavItem = item,
                    onClick = { onItemClick(item) },
                    selected = selectedItem == item
                )
            }
        }
    }
}

@Composable
private fun NavigationButton(
    tabNavItem: TabNavItem,
    onClick: () -> Unit,
    selected: Boolean
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = tabNavItem.icon,
            contentDescription = tabNavItem.title,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}