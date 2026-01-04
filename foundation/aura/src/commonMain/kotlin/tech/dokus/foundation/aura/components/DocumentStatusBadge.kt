package tech.dokus.foundation.aura.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.model.DocumentUiStatus
import tech.dokus.foundation.aura.extensions.color
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.onColor

// Match existing NeedConfirmationBadge dimensions
private val BadgeCornerRadius = 16.dp
private val BadgeHorizontalPadding = 12.dp
private val BadgeVerticalPadding = 4.dp
private val BadgeMinWidth = 80.dp

/**
 * Displays a status badge for document processing state.
 *
 * Color mapping:
 * - Queued: surfaceVariant (neutral, waiting)
 * - Processing: primaryContainer (active, in progress)
 * - Review: secondaryContainer (attention needed, but not error)
 * - Ready: tertiaryContainer (positive, complete)
 * - Failed: errorContainer (error, action needed)
 *
 * @param status The UI status to display
 * @param modifier Optional modifier for customization
 */
@Composable
fun DocumentStatusBadge(
    status: DocumentUiStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = BadgeMinWidth)
            .background(
                color = status.color,
                shape = RoundedCornerShape(BadgeCornerRadius)
            )
            .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.localized,
            style = MaterialTheme.typography.labelSmall,
            color = status.onColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
