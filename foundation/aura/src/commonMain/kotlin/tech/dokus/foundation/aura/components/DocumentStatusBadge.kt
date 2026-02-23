package tech.dokus.foundation.aura.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.model.DocumentUiStatus
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.extensions.statusColor
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Displays a status indicator for document processing state.
 * Uses dot + text pattern.
 *
 * Color mapping:
 * - Queued: onSurfaceVariant (neutral, waiting)
 * - Processing: primary (active, in progress)
 * - Review: secondary (attention needed, but not error)
 * - Ready: tertiary (positive, complete)
 * - Failed: error (error, action needed)
 *
 * @param status The UI status to display
 * @param modifier Optional modifier for customization
 */
@Composable
fun DocumentStatusBadge(
    status: DocumentUiStatus,
    modifier: Modifier = Modifier
) {
    // Dot + text pattern
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(status.statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.localized,
            style = MaterialTheme.typography.labelSmall,
            color = status.statusColor,
            maxLines = 1
        )
    }
}

@Preview
@Composable
private fun DocumentStatusBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentStatusBadge(status = DocumentUiStatus.Ready)
    }
}
