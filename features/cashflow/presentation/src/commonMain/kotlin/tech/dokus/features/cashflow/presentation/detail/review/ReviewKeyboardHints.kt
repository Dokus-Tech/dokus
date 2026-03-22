package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_keyboard_confirm
import tech.dokus.aura.resources.review_keyboard_detail
import tech.dokus.aura.resources.review_keyboard_navigate
import tech.dokus.aura.resources.review_keyboard_zoom
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val KeyShape = RoundedCornerShape(Constraints.Spacing.xSmall)

/**
 * Contextual keyboard shortcut hints displayed at the bottom of the review surface.
 */
@Composable
internal fun ReviewKeyboardHints(
    canConfirm: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyHint(keys = "\u2191\u2193", label = stringResource(Res.string.review_keyboard_navigate))
        if (canConfirm) {
            KeyHint(keys = "Enter", label = stringResource(Res.string.review_keyboard_confirm), highlighted = true)
        }
        KeyHint(keys = "Z", label = stringResource(Res.string.review_keyboard_zoom))
        KeyHint(keys = "D", label = stringResource(Res.string.review_keyboard_detail))
    }
}

@Composable
private fun KeyHint(
    keys: String,
    label: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KeyCap(text = keys, highlighted = highlighted)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
    }
}

@Composable
private fun KeyCap(
    text: String,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val bgColor = if (highlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .clip(KeyShape)
            .background(bgColor)
            .border(1.dp, borderColor, KeyShape)
            .padding(horizontal = Constraints.Spacing.xSmall, vertical = Constraints.Spacing.xxSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
