package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_filter_all
import tech.dokus.aura.resources.documents_filter_confirmed
import tech.dokus.aura.resources.documents_filter_needs_attention
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter

/**
 * Simplified filter buttons for the documents list.
 * Three options: All, Needs attention, Confirmed
 *
 * Uses text buttons with subtle styling instead of FilterChips.
 */
@Composable
internal fun DocumentFilterButtons(
    currentFilter: DocumentFilter,
    needsAttentionCount: Int,
    onFilterSelected: (DocumentFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DocumentFilter.entries.forEach { filter ->
            val isActive = currentFilter == filter
            val count = if (filter == DocumentFilter.NeedsAttention && needsAttentionCount > 0) {
                needsAttentionCount
            } else {
                null
            }

            FilterTextButton(
                text = filter.label,
                isActive = isActive,
                count = count,
                onClick = { onFilterSelected(filter) }
            )
        }
    }
}

@Composable
private fun FilterTextButton(
    text: String,
    isActive: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            } else {
                Color.Transparent
            },
            contentColor = if (isActive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD97706) // Amber for attention count
            )
        }
    }
}

/**
 * Returns the localized label for each filter option.
 */
private val DocumentFilter.label: String
    @Composable get() = when (this) {
        DocumentFilter.All -> stringResource(Res.string.documents_filter_all)
        DocumentFilter.NeedsAttention -> stringResource(Res.string.documents_filter_needs_attention)
        DocumentFilter.Confirmed -> stringResource(Res.string.documents_filter_confirmed)
    }
