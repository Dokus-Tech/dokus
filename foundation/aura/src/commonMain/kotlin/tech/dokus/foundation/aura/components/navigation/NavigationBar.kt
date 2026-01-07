package tech.dokus.foundation.aura.components.navigation

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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow
import tech.dokus.aura.resources.cashflow_title
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.contacts_title
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.nav_banking
import tech.dokus.aura.resources.nav_items
import tech.dokus.aura.resources.nav_profile
import tech.dokus.aura.resources.nav_simulations
import tech.dokus.aura.resources.plus
import tech.dokus.aura.resources.simulations
import tech.dokus.aura.resources.tasks_2
import tech.dokus.aura.resources.user
import tech.dokus.aura.resources.users
import tech.dokus.aura.resources.wallet_2
import tech.dokus.foundation.aura.constrains.Constrains

sealed interface TabNavItem {
    @get:Composable
    val icon: Painter

    @get:Composable
    val title: String
    val route: String
    val showTopBar: Boolean

    data object Today : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.chart_bar_trend_up)
        override val title: String @Composable get() = stringResource(Res.string.home_today)
        override val route: String = "tab/today"
        override val showTopBar: Boolean = false
    }

    data object Contacts : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.users)
        override val title: String @Composable get() = stringResource(Res.string.contacts_title)
        override val route: String = "tab/contacts"
        override val showTopBar: Boolean = false
    }

    data object Cashflow : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.cashflow)
        override val title: String @Composable get() = stringResource(Res.string.cashflow_title)
        override val route: String = "tab/cashflow"
        override val showTopBar: Boolean = false
    }

    data object Simulations : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.simulations)
        override val title: String @Composable get() = stringResource(Res.string.nav_simulations)
        override val route: String = "tab/simulations"
        override val showTopBar: Boolean = false
    }

    data object Items : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.tasks_2)
        override val title: String @Composable get() = stringResource(Res.string.nav_items)
        override val route: String = "tab/inventory"
        override val showTopBar: Boolean = false
    }

    data object Banking : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.wallet_2)
        override val title: String @Composable get() = stringResource(Res.string.nav_banking)
        override val route: String = "tab/banking"
        override val showTopBar: Boolean = false
    }

    data object Profile : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.user)
        override val title: String @Composable get() = stringResource(Res.string.nav_profile)
        override val route: String = "tab/profile"
        override val showTopBar: Boolean = false
    }

    data object AddDocuments : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.plus)
        override val title: String @Composable get() = ""
        override val route: String = "documents/add"
        override val showTopBar: Boolean = false
    }
}

val navItems = listOf(
    TabNavItem.Today,
    TabNavItem.Contacts,
    TabNavItem.Cashflow,
    TabNavItem.Simulations,
    TabNavItem.Items,
    TabNavItem.Banking,
    TabNavItem.Profile
)

fun List<TabNavItem>.findByRoute(route: String): TabNavItem = first {
    it.route == route
}

@Composable
fun NavigationBar(
    items: List<TabNavItem>,
    fabItem: TabNavItem,
    selectedIndex: Int,
    onItemClick: (TabNavItem) -> Unit,
    onFabClick: () -> Unit,
) {
    val middleIndex = items.size / 2
    val (firstHalf, secondHalf) = items.withIndex().partition { it.index < middleIndex }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Constrains.Height.navigationBar)
            .background(MaterialTheme.colorScheme.background),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        firstHalf.forEach {
            AppNavBarItem(it.value, it.index == selectedIndex, onItemClick)
        }
        FloatingActionButton(
            onClick = onFabClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(Constrains.Navigation.fabSize)
        ) {
            Icon(
                painter = fabItem.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(Constrains.IconSize.medium)
            )
        }
        secondHalf.forEach {
            AppNavBarItem(it.value, it.index == selectedIndex, onItemClick)
        }
    }
}

@Composable
fun AppNavBarItem(
    item: TabNavItem,
    isSelected: Boolean,
    onItemClick: (TabNavItem) -> Unit
) {
    val color = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }
    Column(
        modifier = Modifier.padding(horizontal = Constrains.Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onItemClick(item) }
        ) {
            Icon(
                painter = item.icon,
                contentDescription = item.title,
                tint = color,
                modifier = Modifier.size(Constrains.IconSize.medium)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(
                        width = Constrains.Navigation.indicatorWidth,
                        height = Constrains.Navigation.indicatorHeight
                    )
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}
