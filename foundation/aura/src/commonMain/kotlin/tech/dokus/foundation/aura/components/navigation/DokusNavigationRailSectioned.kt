package tech.dokus.foundation.aura.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.navigation.destinations.route

/**
 * Sectioned navigation rail with expandable/collapsible sections (Firstbase style).
 *
 * Features:
 * - Icons only on section headers (parent groups)
 * - Child items are text-only with tree connector lines
 * - Accordion behavior: only one section can be expanded at a time
 * - "Coming soon" items are disabled with reduced opacity
 * - Settings item pinned at bottom
 *
 * @param sections List of navigation sections
 * @param expandedSections Map of section ID to expanded state
 * @param selectedRoute Currently selected route
 * @param settingsItem Optional settings item to show at bottom
 * @param onSectionToggle Called when a section header is clicked
 * @param onItemClick Called when a nav item is clicked
 */
@Composable
fun ColumnScope.DokusNavigationRailSectioned(
    sections: List<NavSection>,
    expandedSections: Map<String, Boolean>,
    selectedRoute: String?,
    settingsItem: NavItem?,
    onSectionToggle: (String) -> Unit,
    onItemClick: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        sections.forEach { section ->
            val isExpanded = expandedSections[section.id] ?: section.defaultExpanded
            val hasSelectedChild = section.items.any { it.destination.route == selectedRoute }

            NavSectionHeader(
                section = section,
                isExpanded = isExpanded,
                isSelected = hasSelectedChild,
                onClick = { onSectionToggle(section.id) }
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    section.items.forEachIndexed { index, item ->
                        val isSelected = item.destination.route == selectedRoute
                        val isLastItem = index == section.items.lastIndex

                        NavItemRow(
                            item = item,
                            isSelected = isSelected,
                            isLastItem = isLastItem,
                            onClick = { if (!item.comingSoon) onItemClick(item) }
                        )
                    }
                }
            }
        }

        // Push settings to bottom
        if (settingsItem != null) {
            Spacer(modifier = Modifier.weight(1f))

            SettingsRow(
                item = settingsItem,
                isSelected = settingsItem.destination.route == selectedRoute,
                onClick = { onItemClick(settingsItem) }
            )
        }
    }
}

@Composable
private fun NavSectionHeader(
    section: NavSection,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Constrains.Spacing.small,
                vertical = Constrains.Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Section icon
        Icon(
            painter = painterResource(section.iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(Constrains.Spacing.small))

        // Section title
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )

        // Chevron on the right
        Icon(
            imageVector = if (isExpanded) {
                Icons.Default.KeyboardArrowDown
            } else {
                Icons.Default.KeyboardArrowRight
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NavItemRow(
    item: NavItem,
    isSelected: Boolean,
    isLastItem: Boolean,
    onClick: () -> Unit
) {
    val itemAlpha = if (item.comingSoon) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.comingSoon, onClick = onClick)
            .padding(vertical = 6.dp)
            .alpha(itemAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tree connector
        TreeConnector(isLastItem = isLastItem)

        // Text only - NO ICON
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected && !item.comingSoon) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        // Coming soon suffix
        if (item.comingSoon) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "· ${stringResource(tech.dokus.aura.resources.Res.string.coming_soon)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TreeConnector(isLastItem: Boolean) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
    ) {
        // Vertical line (extends full height if not last item, half if last)
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(if (isLastItem) 0.5f else 1f)
                .align(Alignment.TopStart)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // Horizontal branch to text
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(1.dp)
                .align(Alignment.CenterStart)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun SettingsRow(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Constrains.Spacing.small,
                vertical = Constrains.Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings icon
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(Constrains.Spacing.small))

        // Settings title
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
