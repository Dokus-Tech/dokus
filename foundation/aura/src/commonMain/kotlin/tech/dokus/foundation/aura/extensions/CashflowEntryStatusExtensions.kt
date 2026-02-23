package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_ledger_status_cancelled
import tech.dokus.aura.resources.cashflow_ledger_status_open
import tech.dokus.aura.resources.cashflow_ledger_status_overdue
import tech.dokus.aura.resources.cashflow_ledger_status_paid
import tech.dokus.domain.enums.CashflowEntryStatus

/**
 * Extension property to get a localized display name for a CashflowEntryStatus.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun StatusDisplay(status: CashflowEntryStatus) {
 *     Text(text = status.localized)
 * }
 * ```
 */
val CashflowEntryStatus.localized: String
    @Composable get() = when (this) {
        CashflowEntryStatus.Open -> stringResource(Res.string.cashflow_ledger_status_open)
        CashflowEntryStatus.Paid -> stringResource(Res.string.cashflow_ledger_status_paid)
        CashflowEntryStatus.Overdue -> stringResource(Res.string.cashflow_ledger_status_overdue)
        CashflowEntryStatus.Cancelled -> stringResource(Res.string.cashflow_ledger_status_cancelled)
    }

/**
 * Extension property to get the status color for dot + text pattern.
 *
 * Color mapping:
 * - Open: statusWarning (amber - action needed)
 * - Paid: statusConfirmed (green - completed)
 * - Overdue: error (red - urgent action needed)
 * - Cancelled: onSurfaceVariant (neutral/muted)
 */
val CashflowEntryStatus.statusColor: Color
    @Composable get() = when (this) {
        CashflowEntryStatus.Open -> MaterialTheme.colorScheme.statusWarning
        CashflowEntryStatus.Paid -> MaterialTheme.colorScheme.statusConfirmed
        CashflowEntryStatus.Overdue -> MaterialTheme.colorScheme.error
        CashflowEntryStatus.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
