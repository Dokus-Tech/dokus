package ai.dokus.foundation.ui.navigation

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
import dokus.foundation.ui.generated.resources.Res
import dokus.foundation.ui.generated.resources.cashflow
import dokus.foundation.ui.generated.resources.chart_bar_trend_up
import dokus.foundation.ui.generated.resources.plus
import dokus.foundation.ui.generated.resources.simulations
import dokus.foundation.ui.generated.resources.tasks_2
import dokus.foundation.ui.generated.resources.user
import dokus.foundation.ui.generated.resources.users
import dokus.foundation.ui.generated.resources.wallet_2


sealed interface TabNavItem {
    @get:Composable
    val icon: Painter

    @get:Composable
    val title: String
    val route: String
    val showTopBar: Boolean

    data object Dashboard : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.chart_bar_trend_up)
        override val title: String @Composable get() = "Dashboard"
        override val route: String = "tab/dashboard"
        override val showTopBar: Boolean = false
    }

    data object Contacts : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.users)
        override val title: String @Composable get() = "Contacts"
        override val route: String = "tab/contacts"
        override val showTopBar: Boolean = false
    }

    data object Cashflow : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.cashflow)
        override val title: String @Composable get() = "Cashflow"
        override val route: String = "tab/cashflow"
        override val showTopBar: Boolean = false
    }

    data object Simulations : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.simulations)
        override val title: String @Composable get() = "Simulations"
        override val route: String = "tab/simulations"
        override val showTopBar: Boolean = false
    }

    data object Items : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.tasks_2)
        override val title: String @Composable get() = "Items"
        override val route: String = "tab/inventory"
        override val showTopBar: Boolean = false
    }

    data object Banking : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.wallet_2)
        override val title: String @Composable get() = "Banking"
        override val route: String = "tab/banking"
        override val showTopBar: Boolean = false
    }

    data object Profile : TabNavItem {
        override val icon: Painter @Composable get() = painterResource(Res.drawable.user)
        override val title: String @Composable get() = "Profile"
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
    TabNavItem.Dashboard,
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
            .height(60.dp)
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
            modifier = Modifier.size(46.dp)
        ) {
            Icon(
                painter = fabItem.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(24.dp)
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
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = { onItemClick(item) }
        ) {
            Icon(
                painter = item.icon,
                contentDescription = item.title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 2.dp)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}