package tech.dokus.foundation.aura.components.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ContainerPadding = 3.dp
private val TabPaddingH = 16.dp
private val TabPaddingV = 6.dp
private val BadgePaddingH = 6.dp
private val BadgePaddingV = 1.dp
private val BadgeGap = 6.dp
private val ActiveShadow = 2.dp

/**
 * Tab data for [DokusTabs].
 */
@Immutable
data class DokusTab(
    val id: String,
    val label: String,
    val count: Int? = null,
    val countColor: Color = Color.Unspecified,
    val countBackground: Color = Color.Unspecified,
)

/**
 * Segmented pill-group tab switcher.
 *
 * Container: canvas background, 7dp outer radius, 1px border.
 * Active tab: page background, shadow, 6dp radius, bold text.
 * Inactive tab: transparent, muted text.
 *
 * @param tabs Tab definitions
 * @param activeId Currently selected tab id
 * @param onTabSelected Callback when a tab is tapped
 */
@Composable
fun DokusTabs(
    tabs: List<DokusTab>,
    activeId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(ContainerPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isActive = tab.id == activeId
                TabItem(
                    tab = tab,
                    isActive = isActive,
                    onClick = { onTabSelected(tab.id) },
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: DokusTab,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(Constraints.CornerRadius.input)

    val backgroundModifier = if (isActive) {
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
    } else {
        Modifier.clip(shape)
    }

    Row(
        modifier = Modifier
            .then(backgroundModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = TabPaddingH, vertical = TabPaddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tab.label,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.textMuted
            },
        )

        if (tab.count != null && tab.count > 0) {
            val badgeColor = if (tab.countColor != Color.Unspecified) {
                tab.countColor
            } else {
                MaterialTheme.colorScheme.textMuted
            }
            val badgeBg = if (tab.countBackground != Color.Unspecified) {
                tab.countBackground
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            Box(
                modifier = Modifier
                    .padding(start = BadgeGap)
                    .background(
                        badgeBg,
                        RoundedCornerShape(Constraints.CornerRadius.badge),
                    )
                    .padding(horizontal = BadgePaddingH, vertical = BadgePaddingV),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.count.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    color = badgeColor,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DokusTabsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusTabs(
            tabs = listOf(
                DokusTab(id = "all", label = "All", count = 12),
                DokusTab(id = "open", label = "Open", count = 3),
                DokusTab(id = "paid", label = "Paid")
            ),
            activeId = "all",
            onTabSelected = {}
        )
    }
}
