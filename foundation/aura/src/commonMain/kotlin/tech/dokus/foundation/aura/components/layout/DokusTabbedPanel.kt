package tech.dokus.foundation.aura.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints

private val CardPadding = Constraints.Spacing.large
private val HeaderSpacing = Constraints.Spacing.medium
private val TabSpacing = Constraints.Spacing.large
private val TabIndicatorHeight = 2.dp
private val ContentSpacing = Constraints.Spacing.medium
private const val ListItemBackgroundAlpha = 0.6f

@Composable
fun <T> DokusTabbedPanel(
    title: String,
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    tabLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (headerAction != null) {
                    headerAction()
                }
            }

            if (tabs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(HeaderSpacing))
                DokusTabRow(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    tabLabel = tabLabel
                )
                Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(ContentSpacing))
            content()
        }
    }
}

@Composable
fun DokusPanelListItem(
    title: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val backgroundColor = containerColor
        ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ListItemBackgroundAlpha)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .then(clickableModifier)
            .padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(Constraints.Spacing.medium))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(Constraints.Spacing.medium))
            trailing()
        }
    }
}

@Composable
private fun <T> DokusTabRow(
    tabs: List<T>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    tabLabel: @Composable (T) -> String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TabSpacing),
        verticalAlignment = Alignment.Bottom
    ) {
        tabs.forEach { tab ->
            DokusTab(
                label = tabLabel(tab),
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun DokusTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = Constraints.Spacing.xxSmall),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = textColor
        )
        Box(
            modifier = Modifier
                .padding(top = Constraints.Spacing.xSmall)
                .height(TabIndicatorHeight)
                .fillMaxWidth()
                .background(indicatorColor)
        )
    }
}
