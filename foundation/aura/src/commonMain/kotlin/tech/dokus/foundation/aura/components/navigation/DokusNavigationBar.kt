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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.more_horizontal
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_more
import tech.dokus.aura.resources.users
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route

/**
 * Standard mobile navigation bar with icons and labels.
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
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        tonalElevation = 0.dp
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = stringResource(tab.titleRes),
                        modifier = Modifier.size(Constraints.IconSize.small)
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
                    indicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.textMuted,
                    unselectedTextColor = MaterialTheme.colorScheme.textMuted
                ),
                onClick = { onTabClick(tab) }
            )
        }
    }
}

@Preview
@Composable
private fun DokusNavigationBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusNavigationBar(
            tabs = listOf(
                MobileTabConfig(
                    id = "documents",
                    titleRes = Res.string.nav_documents,
                    iconRes = Res.drawable.file_text,
                    destination = HomeDestination.Documents,
                ),
                MobileTabConfig(
                    id = "contacts",
                    titleRes = Res.string.nav_contacts,
                    iconRes = Res.drawable.users,
                    destination = HomeDestination.Contacts,
                ),
                MobileTabConfig(
                    id = "more",
                    titleRes = Res.string.nav_more,
                    iconRes = Res.drawable.more_horizontal,
                    destination = null,
                ),
            ),
            selectedRoute = "documents",
            onTabClick = {},
        )
    }
}
