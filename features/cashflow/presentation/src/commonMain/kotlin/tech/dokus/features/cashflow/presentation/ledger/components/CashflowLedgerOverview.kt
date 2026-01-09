package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_ledger_items
import tech.dokus.aura.resources.cashflow_ledger_status_cancelled
import tech.dokus.aura.resources.cashflow_ledger_status_open
import tech.dokus.aura.resources.cashflow_ledger_status_overdue
import tech.dokus.aura.resources.cashflow_ledger_status_paid
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.model.CashflowEntry

private val OverviewBarHeight = 8.dp
private val OverviewBarRadius = 10.dp

@Immutable
private data class StatusSummary(
    val status: CashflowEntryStatus,
    val label: String,
    val color: Color,
    val amountMinor: Long,
    val count: Int
)

@Composable
internal fun CashflowLedgerOverview(
    entries: List<CashflowEntry>,
    modifier: Modifier = Modifier
) {
    val summaries = buildStatusSummaries(entries)

    CashflowOverviewBar(
        summaries = summaries,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun CashflowOverviewBar(
    summaries: List<StatusSummary>,
    modifier: Modifier = Modifier
) {
    // Always use amount for bar proportions when available
    val hasAnyAmount = summaries.any { it.amountMinor != 0L }
    val totalValue = summaries.sumOf { if (hasAnyAmount) it.amountMinor else it.count.toLong() }
    val barBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(OverviewBarHeight)
                .background(barBackground, RoundedCornerShape(OverviewBarRadius))
                .clip(RoundedCornerShape(OverviewBarRadius))
        ) {
            if (totalValue > 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    summaries.forEach { summary ->
                        val segmentValue = if (hasAnyAmount) summary.amountMinor else summary.count.toLong()
                        if (segmentValue > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(segmentValue.toFloat())
                                    .height(OverviewBarHeight)
                                    .background(summary.color)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            summaries.forEach { summary ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Status label
                    Text(
                        text = summary.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Amount as primary (or "—" if 0)
                    Text(
                        text = if (summary.amountMinor != 0L) {
                            Money(summary.amountMinor).toDisplayString()
                        } else {
                            "—"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (summary.amountMinor != 0L) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    // Count as secondary (always shown, muted)
                    Text(
                        text = stringResource(Res.string.cashflow_ledger_items, summary.count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun buildStatusSummaries(entries: List<CashflowEntry>): List<StatusSummary> {
    val grouped = entries.groupBy { it.status }
    return listOf(
        summaryForStatus(
            status = CashflowEntryStatus.Open,
            label = stringResource(Res.string.cashflow_ledger_status_open),
            color = MaterialTheme.colorScheme.primary,
            entries = grouped[CashflowEntryStatus.Open].orEmpty()
        ),
        summaryForStatus(
            status = CashflowEntryStatus.Overdue,
            label = stringResource(Res.string.cashflow_ledger_status_overdue),
            color = MaterialTheme.colorScheme.error,
            entries = grouped[CashflowEntryStatus.Overdue].orEmpty()
        ),
        summaryForStatus(
            status = CashflowEntryStatus.Paid,
            label = stringResource(Res.string.cashflow_ledger_status_paid),
            color = MaterialTheme.colorScheme.tertiary,
            entries = grouped[CashflowEntryStatus.Paid].orEmpty()
        ),
        summaryForStatus(
            status = CashflowEntryStatus.Cancelled,
            label = stringResource(Res.string.cashflow_ledger_status_cancelled),
            color = MaterialTheme.colorScheme.outline,
            entries = grouped[CashflowEntryStatus.Cancelled].orEmpty()
        )
    )
}

@Composable
private fun summaryForStatus(
    status: CashflowEntryStatus,
    label: String,
    color: Color,
    entries: List<CashflowEntry>
): StatusSummary {
    val amountMinor = entries.sumOf { it.amountGross.minor }
    return StatusSummary(
        status = status,
        label = label,
        color = color,
        amountMinor = amountMinor,
        count = entries.size
    )
}
