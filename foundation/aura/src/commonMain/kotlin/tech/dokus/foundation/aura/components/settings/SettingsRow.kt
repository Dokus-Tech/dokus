package tech.dokus.foundation.aura.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val ChevronChar = "\u203A" // ›

/**
 * iOS Settings-style key-value row.
 *
 * Uses: Profile screen (Account, Security, Server, Danger Zone).
 *
 * @param label Left text
 * @param value Right-side value string
 * @param mono Monospace value
 * @param chevron Shows `›` arrow (implies tappable)
 * @param destructive Red text, 500 weight
 * @param showDivider Bottom border (default true)
 * @param onClick Tap callback
 * @param trailing Custom right-side composable (StatusDot, badge, etc.)
 */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    mono: Boolean = false,
    chevron: Boolean = false,
    destructive: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val isLarge = LocalScreenSize.current.isLarge
    val paddingH = if (isLarge) 18.dp else 16.dp
    val paddingV = if (isLarge) 12.dp else 13.dp
    val labelSize = if (isLarge) 12.5.sp else 13.sp

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val showHover = isHovered && (onClick != null || chevron)
    val hoverColor = MaterialTheme.colorScheme.surfaceHover

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier.hoverable(interactionSource)
    }

    val labelColor = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val labelWeight = if (destructive) FontWeight.Medium else FontWeight.Normal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .drawBehind {
                if (showHover) {
                    drawRect(hoverColor)
                }
            }
            .padding(horizontal = paddingH, vertical = paddingV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            fontSize = labelSize,
            fontWeight = labelWeight,
            color = labelColor,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            if (trailing != null) {
                trailing()
            }

            if (value != null) {
                Text(
                    text = value,
                    fontSize = labelSize,
                    color = MaterialTheme.colorScheme.textMuted,
                    fontFamily = if (mono) MaterialTheme.typography.labelLarge.fontFamily else null,
                )
            }

            if (chevron) {
                Text(
                    text = ChevronChar,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.textFaint,
                )
            }
        }
    }

    if (showDivider) {
        HorizontalDivider(
            thickness = Constraints.Stroke.thin,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Preview
@Composable
private fun SettingsRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SettingsRow(
            label = "Language",
            value = "English",
            chevron = true,
            onClick = {}
        )
    }
}
