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
import tech.dokus.domain.enums.CashflowEntryStatus
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.extensions.statusColor
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Displays a status indicator for cashflow entry state.
 * Uses dot + text pattern.
 *
 * Color mapping:
 * - Open: tertiary (amber/warning - action needed)
 * - Paid: primary (green/success - completed)
 * - Overdue: error (red - urgent action needed)
 * - Cancelled: onSurfaceVariant (neutral/muted)
 *
 * @param status The cashflow entry status to display
 * @param detail Optional detail text (e.g., "Due in 5 days", "3 days overdue")
 * @param modifier Optional modifier for customization
 */
@Composable
fun CashflowStatusBadge(
    status: CashflowEntryStatus,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
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
            text = if (detail != null) "${status.localized} - $detail" else status.localized,
            style = MaterialTheme.typography.labelMedium,
            color = status.statusColor,
            maxLines = 1
        )
    }
}

@Preview
@Composable
private fun CashflowStatusBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CashflowStatusBadge(status = CashflowEntryStatus.Open)
    }
}
