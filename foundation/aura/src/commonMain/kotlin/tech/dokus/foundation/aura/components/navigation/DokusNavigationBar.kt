package tech.dokus.foundation.aura.components.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.navigation.destinations.route

/**
 * Navigation bar for mobile using MobileTabConfig with route-based selection.
 */
@Composable
fun DokusNavigationBar(
    tabs: List<MobileTabConfig>,
    selectedRoute: String?,
    onTabClick: (MobileTabConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = stringResource(tab.titleRes),
                        modifier = Modifier.size(Constrains.IconSize.small)
                    )
                },
                label = {
                    Text(
                        stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selectedRoute == tab.destination?.route,
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = { onTabClick(tab) }
            )
        }
    }
}
