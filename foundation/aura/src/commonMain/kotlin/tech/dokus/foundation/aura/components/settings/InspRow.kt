package tech.dokus.foundation.aura.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.components.status.ConfDot
import tech.dokus.foundation.aura.components.status.ConfidenceLevel
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val EditablePaddingH = 8.dp
private val EditablePaddingV = 4.dp

/**
 * Inspector panel key-value row with optional confidence indicator.
 *
 * Uses: Document inspector invoice detail fields.
 *
 * @param label Field label
 * @param value Field value (null shows "—")
 * @param mono Monospace value font
 * @param confidence Shows ConfDot if provided
 * @param editable Input container styling
 * @param warn Amber border for missing required fields
 */
@Composable
fun InspRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    confidence: ConfidenceLevel? = null,
    editable: Boolean = false,
    warn: Boolean = false,
) {
    val shape = RoundedCornerShape(Constraints.CornerRadius.input)

    Column(
        modifier = modifier.padding(vertical = 7.dp),
    ) {
        // Label row with optional confidence dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            modifier = Modifier.padding(bottom = Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.textMuted,
            )
            if (confidence != null) {
                ConfDot(level = confidence)
            }
        }

        // Value with optional editable container
        val valueModifier = if (editable) {
            val borderColor = if (warn) {
                MaterialTheme.colorScheme.borderAmber
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = EditablePaddingH, vertical = EditablePaddingV)
        } else {
            Modifier
        }

        val displayValue = value ?: "\u2014" // em dash
        val valueColor = when {
            value == null -> MaterialTheme.colorScheme.textFaint
            warn -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
        val valueFamily = if (mono) {
            MaterialTheme.typography.labelLarge.fontFamily
        } else {
            null
        }

        Text(
            text = displayValue,
            modifier = valueModifier,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            fontFamily = valueFamily,
        )
    }
}
