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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A chip displaying the document status with appropriate colors.
 */
@Composable
internal fun DocumentStatusChip(
    status: DocumentDisplayStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, label) = when (status) {
        DocumentDisplayStatus.Processing -> Triple(
            Color(0xFFFFF3E0), // Light orange
            Color(0xFFE65100), // Dark orange
            "Processing"
        )
        DocumentDisplayStatus.NeedsReview -> Triple(
            Color(0xFFE3F2FD), // Light blue
            Color(0xFF1565C0), // Dark blue
            "Needs Review"
        )
        DocumentDisplayStatus.Ready -> Triple(
            Color(0xFFE8F5E9), // Light green
            Color(0xFF2E7D32), // Dark green
            "Ready"
        )
        DocumentDisplayStatus.Confirmed -> Triple(
            Color(0xFFE0F2F1), // Light teal
            Color(0xFF00695C), // Dark teal
            "Confirmed"
        )
        DocumentDisplayStatus.Failed -> Triple(
            Color(0xFFFFEBEE), // Light red
            Color(0xFFC62828), // Dark red
            "Failed"
        )
        DocumentDisplayStatus.Rejected -> Triple(
            Color(0xFFFCE4EC), // Light pink
            Color(0xFFAD1457), // Dark pink
            "Rejected"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
