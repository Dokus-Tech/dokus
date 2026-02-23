package tech.dokus.foundation.aura.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val ChevronChar = "\u203A" // ›
private const val ChevronRotation = 90f

/**
 * Expandable section with toggle chevron.
 *
 * @param title Section title
 * @param isExpanded Current expanded state
 * @param onToggle Toggle callback
 * @param right Optional right-side metadata text
 */
@Composable
fun PCollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    right: String? = null,
    content: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(if (isExpanded) ChevronRotation else 0f)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = Constraints.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            Text(
                text = ChevronChar,
                modifier = Modifier.rotate(rotation),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.textMuted,
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (right != null) {
                Text(
                    text = right,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        // Bottom border
        HorizontalDivider(
            thickness = Constraints.Stroke.thin,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(bottom = Constraints.Spacing.large)) {
                content()
            }
        }
    }
}

@Preview
@Composable
private fun PCollapsibleSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PCollapsibleSection(
            title = "Details",
            isExpanded = true,
            onToggle = {},
            right = "3 items"
        ) {
            Text("Section content")
        }
    }
}
