package ai.thepredict.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    data object Charts : NavigationItem {
        override val icon: DrawableResource = Res.drawable.chart_bar_trend_up
        override val label: String @Composable get() = "Charts"
        override val route: String = "charts"
    }

    data object Contacts : NavigationItem {
        override val icon: DrawableResource = Res.drawable.users
        override val label: String @Composable get() = "Contacts"
        override val route: String = "contacts"
    }

    data object Inventory : NavigationItem {
        override val icon: DrawableResource = Res.drawable.tasks_2
        override val label: String @Composable get() = "Items"
        override val route: String = "items"
    }

    data object Banks : NavigationItem {
        override val icon: DrawableResource = Res.drawable.wallet_2
        override val label: String @Composable get() = "Banks"
        override val route: String = "banks"
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
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    val selectedItem = navigationItems[selectedIndex]
    val half = navigationItems.size / 2
    val firstHalf = navigationItems.take(half)
    val secondHalf = navigationItems.drop(half)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            firstHalf.forEach { item ->
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        tint = if (selectedItem == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Add button (FAB)
            FloatingActionButton(
                onClick = { },
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(Res.drawable.plus),
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp)
                )
            }

            secondHalf.forEach { item ->
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        tint = if (selectedItem == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}