package ai.dokus.app.cashflow.components.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate

/**
 * Dates section in the invoice document.
 * Shows issue date and due date side by side, both clickable to open date pickers.
 */
@Composable
fun InvoiceDatesSection(
    issueDate: LocalDate?,
    dueDate: LocalDate?,
    onIssueDateClick: () -> Unit,
    onDueDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        DateField(
            label = "ISSUE DATE",
            date = issueDate,
            onClick = onIssueDateClick,
            modifier = Modifier.weight(1f)
        )
        DateField(
            label = "DUE DATE",
            date = dueDate,
            onClick = onDueDateClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isHovered) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        Text(
            text = date?.formatDate() ?: "Click to set",
            style = MaterialTheme.typography.bodyMedium,
            color = if (date != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        // Hover hint
        if (isHovered) {
            Text(
                text = "Click to change",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Format LocalDate to a readable string.
 * Format: "Dec 13, 2025"
 */
private fun LocalDate.formatDate(): String {
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val monthName = monthNames.getOrElse(month.ordinal) { "???" }
    return "$monthName $day, $year"
}
