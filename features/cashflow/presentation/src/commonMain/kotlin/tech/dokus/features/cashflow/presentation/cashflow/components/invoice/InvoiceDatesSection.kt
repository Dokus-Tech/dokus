package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.date_format_short
import tech.dokus.aura.resources.date_month_short_apr
import tech.dokus.aura.resources.date_month_short_aug
import tech.dokus.aura.resources.date_month_short_dec
import tech.dokus.aura.resources.date_month_short_feb
import tech.dokus.aura.resources.date_month_short_jan
import tech.dokus.aura.resources.date_month_short_jul
import tech.dokus.aura.resources.date_month_short_jun
import tech.dokus.aura.resources.date_month_short_mar
import tech.dokus.aura.resources.date_month_short_may
import tech.dokus.aura.resources.date_month_short_nov
import tech.dokus.aura.resources.date_month_short_oct
import tech.dokus.aura.resources.date_month_short_sep
import tech.dokus.aura.resources.invoice_click_to_change
import tech.dokus.aura.resources.invoice_click_to_set
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
            label = stringResource(Res.string.invoice_issue_date),
            date = issueDate,
            onClick = onIssueDateClick,
            modifier = Modifier.weight(1f)
        )
        DateField(
            label = stringResource(Res.string.invoice_due_date),
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
            text = date?.formatDate() ?: stringResource(Res.string.invoice_click_to_set),
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
                text = stringResource(Res.string.invoice_click_to_change),
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
@Composable
private fun LocalDate.formatDate(): String {
    val monthNames = listOf(
        stringResource(Res.string.date_month_short_jan),
        stringResource(Res.string.date_month_short_feb),
        stringResource(Res.string.date_month_short_mar),
        stringResource(Res.string.date_month_short_apr),
        stringResource(Res.string.date_month_short_may),
        stringResource(Res.string.date_month_short_jun),
        stringResource(Res.string.date_month_short_jul),
        stringResource(Res.string.date_month_short_aug),
        stringResource(Res.string.date_month_short_sep),
        stringResource(Res.string.date_month_short_oct),
        stringResource(Res.string.date_month_short_nov),
        stringResource(Res.string.date_month_short_dec),
    )
    val monthName = monthNames.getOrElse(month.ordinal) { stringResource(Res.string.common_unknown) }
    return stringResource(Res.string.date_format_short, monthName, day, year)
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun InvoiceDatesSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceDatesSection(
            issueDate = LocalDate(2024, 12, 13),
            dueDate = LocalDate(2025, 1, 13),
            onIssueDateClick = {},
            onDueDateClick = {}
        )
    }
}
