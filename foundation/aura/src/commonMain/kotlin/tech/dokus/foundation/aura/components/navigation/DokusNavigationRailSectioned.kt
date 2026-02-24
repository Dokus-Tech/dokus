package tech.dokus.foundation.aura.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.calculator
import tech.dokus.aura.resources.coming_soon
import tech.dokus.aura.resources.file_text
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.aura.resources.nav_documents
import tech.dokus.aura.resources.nav_profile
import tech.dokus.aura.resources.nav_section_accounting
import tech.dokus.aura.resources.nav_section_company
import tech.dokus.aura.resources.nav_vat
import tech.dokus.aura.resources.settings
import tech.dokus.aura.resources.users
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusRadii
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route

@Composable
fun ColumnScope.DokusNavigationRailSectioned(
    sections: List<NavSection>,
    pinnedItems: List<NavItem>,
    expandedSections: Map<String, Boolean>,
    selectedRoute: String?,
    settingsItem: NavItem?,
    onSectionToggle: (String) -> Unit,
    onItemClick: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        if (pinnedItems.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(sizing.strokeThin)) {
                pinnedItems.forEach { item ->
                    PinnedNavItemRow(
                        item = item,
                        isSelected = item.destination.route == selectedRoute,
                        onClick = { if (!item.comingSoon) onItemClick(item) }
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.dokusEffects.railTrackLine,
                modifier = Modifier.padding(horizontal = spacing.small)
            )
        }

        sections.forEach { section ->
            val isExpanded = expandedSections[section.id] ?: section.defaultExpanded
            val hasSelectedChild = section.items.any { it.destination.route == selectedRoute }

            Column(verticalArrangement = Arrangement.spacedBy(sizing.strokeThin)) {
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
                    val trackColor = MaterialTheme.dokusEffects.railTrackLine
                    Column(
                        modifier = Modifier
                            .padding(start = spacing.large)
                            .drawBehind {
                                // Left border track line
                                drawLine(
                                    color = trackColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = sizing.strokeThin.toPx()
                                )
                            },
                        verticalArrangement = Arrangement.spacedBy(sizing.strokeThin)
                    ) {
                        section.items.forEach { item ->
                            val isSelected = item.destination.route == selectedRoute
                            NavItemRow(
                                item = item,
                                isSelected = isSelected,
                                onClick = { if (!item.comingSoon) onItemClick(item) }
                            )
                        }
                    }
                }
            }
        }

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
private fun PinnedNavItemRow(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spacing = MaterialTheme.dokusSpacing
    val radii = MaterialTheme.dokusRadii
    val backgroundColor = if (isSelected) {
        MaterialTheme.dokusEffects.railActiveBackground
    } else {
        MaterialTheme.colorScheme.surface
    }
    val titleColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.button))
            .background(backgroundColor)
            .clickable(enabled = !item.comingSoon, onClick = onClick)
            .padding(horizontal = spacing.small, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dokusSizing.iconXSmall),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(spacing.small))

        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = titleColor,
            modifier = Modifier.weight(1f)
        )

        item.desktopShortcutHint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textFaint,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.dokusEffects.railBadgeBackground)
                    .padding(horizontal = spacing.xSmall, vertical = spacing.xxSmall)
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
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.small, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotating chevron
        Text(
            text = "\u203A", // ›
            style = MaterialTheme.typography.bodyLarge,
            color = iconTint,
            modifier = Modifier.rotate(rotation)
        )

        Spacer(modifier = Modifier.width(spacing.xSmall))

        // Section icon
        androidx.compose.material3.Icon(
            painter = painterResource(section.iconRes),
            contentDescription = null,
            modifier = Modifier.size(sizing.iconXSmall),
            tint = iconTint
        )

        Spacer(modifier = Modifier.width(spacing.small))

        // Section title
        Text(
            text = stringResource(section.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NavItemRow(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val radii = MaterialTheme.dokusRadii
    val itemAlpha = if (item.comingSoon) 0.5f else 1f
    val itemShape = RoundedCornerShape(topEnd = radii.button, bottomEnd = radii.button)
    val activeBg = MaterialTheme.dokusEffects.railActiveBackground
    val amberBorder = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
            .then(
                if (isSelected && !item.comingSoon) {
                    Modifier
                        .clip(itemShape)
                        .background(activeBg)
                        .drawBehind {
                            // 2px amber left border
                            drawLine(
                                color = amberBorder,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = sizing.navigationIndicatorHeight.toPx()
                            )
                        }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !item.comingSoon, onClick = onClick)
            .padding(
                start = spacing.large,
                top = spacing.medium,
                bottom = spacing.medium,
                end = spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(item.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected && !item.comingSoon) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected && !item.comingSoon) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        if (item.comingSoon) {
            Spacer(modifier = Modifier.width(spacing.xSmall))
            Text(
                text = stringResource(Res.string.coming_soon),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textFaint,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.dokusEffects.railBadgeBackground)
                    .padding(horizontal = spacing.xSmall, vertical = spacing.xxSmall)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = spacing.small,
                vertical = spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(sizing.iconSmall),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Spacer(modifier = Modifier.width(spacing.small))

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

@Preview
@Composable
private fun DokusNavigationRailSectionedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        Column {
            DokusNavigationRailSectioned(
                sections = listOf(
                    NavSection(
                        id = "accounting",
                        titleRes = Res.string.nav_section_accounting,
                        iconRes = Res.drawable.file_text,
                        items = listOf(
                            NavItem(
                                id = "documents",
                                titleRes = Res.string.nav_documents,
                                iconRes = Res.drawable.file_text,
                                destination = HomeDestination.Documents,
                            ),
                            NavItem(
                                id = "vat",
                                titleRes = Res.string.nav_vat,
                                iconRes = Res.drawable.calculator,
                                destination = HomeDestination.Tomorrow,
                                comingSoon = true,
                            ),
                        ),
                    ),
                    NavSection(
                        id = "company",
                        titleRes = Res.string.nav_section_company,
                        iconRes = Res.drawable.users,
                        items = listOf(
                            NavItem(
                                id = "contacts",
                                titleRes = Res.string.nav_contacts,
                                iconRes = Res.drawable.users,
                                destination = HomeDestination.Contacts,
                            ),
                        ),
                    ),
                ),
                pinnedItems = listOf(
                    NavItem(
                        id = "search",
                        titleRes = Res.string.action_search,
                        iconRes = Res.drawable.file_text,
                        destination = HomeDestination.Search,
                        desktopShortcutHint = "⌘K",
                    )
                ),
                expandedSections = mapOf("accounting" to true, "company" to true),
                selectedRoute = "documents",
                settingsItem = NavItem(
                    id = "settings",
                    titleRes = Res.string.nav_profile,
                    iconRes = Res.drawable.settings,
                    destination = HomeDestination.Settings,
                ),
                onSectionToggle = {},
                onItemClick = {},
            )
        }
    }
}
