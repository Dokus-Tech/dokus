package tech.dokus.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.navigation.NavDefinition
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

/**
 * More screen for mobile navigation.
 * Shows grouped navigation items that don't fit in the bottom bar.
 */
@Composable
internal fun MoreScreen() {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(Constrains.Spacing.large)
    ) {
        NavDefinition.sections.forEach { section ->
            MoreSectionHeader(section = section)
            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            section.items.forEach { item ->
                MoreNavItem(
                    item = item,
                    onClick = {
                        if (!item.comingSoon) {
                            NavDefinition.routeToDestination(item.route)?.let { destination ->
                                navController.navigateTo(destination)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(Constrains.Spacing.large))
        }
    }
}

@Composable
private fun MoreSectionHeader(section: NavSection) {
    Text(
        text = stringResource(section.titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = Constrains.Spacing.small)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun MoreNavItem(
    item: NavItem,
    onClick: () -> Unit
) {
    val alpha = if (item.comingSoon) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.comingSoon, onClick = onClick)
            .padding(vertical = Constrains.Spacing.medium)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(Constrains.IconSize.medium),
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(Constrains.Spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.comingSoon) {
                Text(
                    text = "Coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
