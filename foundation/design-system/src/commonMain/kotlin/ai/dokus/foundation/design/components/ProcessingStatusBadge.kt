package ai.dokus.foundation.design.components

import tech.dokus.domain.enums.ProcessingStatus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A specialized status badge for document processing states.
 * Maps ProcessingStatus enum values to appropriate colors for visual indication.
 *
 * Color mapping:
 * - Pending: Neutral gray - document awaiting processing
 * - Queued: Blue-gray - document in message queue
 * - Processing: Primary blue - active AI extraction
 * - Processed: Success green - ready for user review
 * - Failed: Error red - extraction failed (retryable)
 * - Confirmed: Success green - user confirmed, entity created
 * - Rejected: Warning amber - user rejected extraction
 *
 * @param status The processing status to display
 * @param modifier Optional modifier for the badge
 */
@Composable
fun ProcessingStatusBadge(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, displayText) = getStatusColors(status)

    StatusBadge(
        text = displayText,
        backgroundColor = backgroundColor,
        textColor = textColor,
        modifier = modifier
    )
}

/**
 * Returns the color configuration for a given processing status.
 * Uses Material Theme colors for consistent theming across light/dark modes.
 */
@Composable
private fun getStatusColors(status: ProcessingStatus): StatusColorConfig {
    val colorScheme = MaterialTheme.colorScheme

    return when (status) {
        ProcessingStatus.Pending -> StatusColorConfig(
            backgroundColor = colorScheme.surfaceVariant,
            textColor = colorScheme.onSurfaceVariant,
            displayText = "Pending"
        )
        ProcessingStatus.Queued -> StatusColorConfig(
            backgroundColor = colorScheme.secondaryContainer,
            textColor = colorScheme.onSecondaryContainer,
            displayText = "Queued"
        )
        ProcessingStatus.Processing -> StatusColorConfig(
            backgroundColor = colorScheme.primaryContainer,
            textColor = colorScheme.onPrimaryContainer,
            displayText = "Processing"
        )
        ProcessingStatus.Processed -> StatusColorConfig(
            backgroundColor = colorScheme.tertiaryContainer,
            textColor = colorScheme.onTertiaryContainer,
            displayText = "Ready for Review"
        )
        ProcessingStatus.Failed -> StatusColorConfig(
            backgroundColor = colorScheme.errorContainer,
            textColor = colorScheme.onErrorContainer,
            displayText = "Failed"
        )
        ProcessingStatus.Confirmed -> StatusColorConfig(
            backgroundColor = colorScheme.tertiaryContainer,
            textColor = colorScheme.onTertiaryContainer,
            displayText = "Confirmed"
        )
        ProcessingStatus.Rejected -> StatusColorConfig(
            backgroundColor = colorScheme.surfaceVariant.copy(alpha = 0.7f),
            textColor = colorScheme.onSurfaceVariant,
            displayText = "Rejected"
        )
    }
}

/**
 * Data class holding color configuration for a status badge.
 */
private data class StatusColorConfig(
    val backgroundColor: Color,
    val textColor: Color,
    val displayText: String
)
