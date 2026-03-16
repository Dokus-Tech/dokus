package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.ai.TransactionReference
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.math.abs

private val ChipShape = RoundedCornerShape(6.dp)
private val StatusDotSize = 5.dp

/**
 * Transaction reference chip for AI chat responses.
 * Shows status dot, description, date, signed amount, and status badge.
 */
@Composable
fun ChatTransactionChip(
    tx: TransactionReference,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (tx.status) {
        "unmatched" -> MaterialTheme.colorScheme.statusError
        "matched" -> MaterialTheme.colorScheme.statusConfirmed
        else -> MaterialTheme.colorScheme.primary // "review"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ChipShape,
            )
            .border(Constraints.Stroke.thin, MaterialTheme.colorScheme.outlineVariant, ChipShape)
            .padding(horizontal = Constraints.Spacing.small, vertical = Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(StatusDotSize)
                .background(statusColor, CircleShape),
        )

        // Description
        Text(
            text = tx.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Date
        tx.date?.let { date ->
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
            )
        }

        // Amount
        Text(
            text = "\u2212\u20ac${String.format("%.2f", abs(tx.amount))}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.statusError,
        )

        // Status badge
        Text(
            text = tx.status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.06f), RoundedCornerShape(3.dp))
                .padding(horizontal = Constraints.Spacing.xSmall, vertical = Constraints.Spacing.xxSmall),
        )
    }
}

@Preview
@Composable
private fun ChatTransactionChipPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatTransactionChip(
            tx = TransactionReference(
                description = "Adobe Creative Cloud",
                amount = -59.99,
                status = "unmatched",
                date = "Mar 2",
            )
        )
    }
}
