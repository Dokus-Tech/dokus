package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_status_failed
import tech.dokus.aura.resources.document_status_processing
import tech.dokus.aura.resources.draft_status_confirmed
import tech.dokus.aura.resources.draft_status_needs_review
import tech.dokus.aura.resources.draft_status_ready
import tech.dokus.aura.resources.draft_status_rejected
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusProcessing
import tech.dokus.foundation.aura.style.statusWarning

/**
 * A chip displaying the document status with appropriate colors.
 */
@Composable
internal fun DocumentStatusChip(
    status: DocumentDisplayStatus,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (statusColor, labelRes) = when (status) {
        DocumentDisplayStatus.Processing -> colorScheme.statusProcessing to Res.string.document_status_processing
        DocumentDisplayStatus.NeedsReview -> colorScheme.statusWarning to Res.string.draft_status_needs_review
        DocumentDisplayStatus.Ready -> colorScheme.statusConfirmed to Res.string.draft_status_ready
        DocumentDisplayStatus.Confirmed -> colorScheme.statusConfirmed to Res.string.draft_status_confirmed
        DocumentDisplayStatus.Failed -> colorScheme.statusError to Res.string.document_status_failed
        DocumentDisplayStatus.Rejected -> colorScheme.statusError to Res.string.draft_status_rejected
    }
    val backgroundColor = statusColor.copy(alpha = 0.12f)
    val textColor = statusColor
    val label = stringResource(labelRes)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Constrains.CornerRadius.sm))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
