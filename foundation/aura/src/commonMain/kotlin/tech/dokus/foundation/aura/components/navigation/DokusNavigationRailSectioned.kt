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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.coming_soon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.style.isDark
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.navigation.destinations.route

// v2 nav rail: left border track, rotating chevron, amber active highlight

private val ItemShape = RoundedCornerShape(
    topEnd = Constrains.CornerRadius.button, bottomEnd = Constrains.CornerRadius.button
)
private val ActiveBorderWidth = 2.dp
private val TrackLineAlpha = 0.06f

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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        sections.forEach { section ->
            val isExpanded = expandedSections[section.id] ?: section.defaultExpanded
            val hasSelectedChild = section.items.any { it.destination.route == selectedRoute }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
                    val trackColor = Color.Black.copy(alpha = TrackLineAlpha)
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .drawBehind {
                                // Left border track line
                                drawLine(
                                    color = trackColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            },
                        verticalArrangement = Arrangement.spacedBy(1.dp)
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
private fun NavSectionHeader(
    section: NavSection,
    isExpanded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            .padding(horizontal = Constrains.Spacing.small, vertical = Constrains.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotating chevron
        Text(
            text = "\u203A", // ›
            style = MaterialTheme.typography.bodyLarge,
            color = iconTint,
            modifier = Modifier.rotate(rotation)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Section icon
        androidx.compose.material3.Icon(
            painter = painterResource(section.iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconTint
        )

        Spacer(modifier = Modifier.width(Constrains.Spacing.small))

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
    val isDark = MaterialTheme.colorScheme.isDark
    val itemAlpha = if (item.comingSoon) 0.5f else 1f
    val activeBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
    val amberBorder = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(itemAlpha)
            .then(
                if (isSelected && !item.comingSoon) {
                    Modifier
                        .clip(ItemShape)
                        .background(activeBg)
                        .drawBehind {
                            // 2px amber left border
                            drawLine(
                                color = amberBorder,
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                strokeWidth = ActiveBorderWidth.toPx()
                            )
                        }
                } else {
                    Modifier
                }
            )
            .clickable(enabled = !item.comingSoon, onClick = onClick)
            .padding(start = 12.dp, top = 7.dp, bottom = 7.dp, end = 8.dp),
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
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.coming_soon),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = MaterialTheme.colorScheme.textFaint,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color.Black.copy(alpha = 0.03f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
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
        androidx.compose.material3.Icon(
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
