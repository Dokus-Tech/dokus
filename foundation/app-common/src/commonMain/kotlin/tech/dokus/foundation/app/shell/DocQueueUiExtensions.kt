package tech.dokus.foundation.app.shell

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
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
import tech.dokus.aura.resources.cashflow_detail_days_overdue
import tech.dokus.aura.resources.document_detail_needs_review
import tech.dokus.aura.resources.document_status_processing
import tech.dokus.aura.resources.payable_invoice_status_overdue
import tech.dokus.aura.resources.payable_invoice_status_pending
import tech.dokus.aura.resources.payable_invoice_status_paid
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

val DocQueueStatus.localized: String
    @Composable get() = when (this) {
        DocQueueStatus.Paid -> stringResource(Res.string.payable_invoice_status_paid)
        DocQueueStatus.Unpaid -> stringResource(Res.string.payable_invoice_status_pending)
        DocQueueStatus.Overdue -> stringResource(Res.string.payable_invoice_status_overdue)
        DocQueueStatus.Review -> stringResource(Res.string.document_detail_needs_review)
        DocQueueStatus.Processing -> stringResource(Res.string.document_status_processing)
    }

val DocQueueStatus.colorized: Color
    @Composable get() = when (this) {
        DocQueueStatus.Paid -> MaterialTheme.colorScheme.statusConfirmed
        DocQueueStatus.Overdue -> MaterialTheme.colorScheme.statusError
        DocQueueStatus.Review -> MaterialTheme.colorScheme.statusWarning
        DocQueueStatus.Unpaid -> MaterialTheme.colorScheme.textMuted
        DocQueueStatus.Processing -> MaterialTheme.colorScheme.statusWarning
    }

val DocQueueStatus.dotType: StatusDotType
    get() = when (this) {
        DocQueueStatus.Paid -> StatusDotType.Confirmed
        DocQueueStatus.Overdue -> StatusDotType.Error
        DocQueueStatus.Review -> StatusDotType.Warning
        DocQueueStatus.Unpaid -> StatusDotType.Empty
        DocQueueStatus.Processing -> StatusDotType.Warning
    }

val DocQueueStatusDetail.localized: String
    @Composable get() = when (this) {
        DocQueueStatusDetail.Processing -> stringResource(Res.string.document_status_processing)
        is DocQueueStatusDetail.OverdueDays -> stringResource(Res.string.cashflow_detail_days_overdue, days)
    }

val DocQueueItem.statusLocalized: String
    @Composable get() = statusDetail?.localized ?: status.localized

val DocQueueItem.amountLocalized: String
    get() = amount?.let { "${currency.displaySign}${it.toDisplayString()}" } ?: "\u2014"

val DocQueueItem.dateLocalized: String
    @Composable get() = date.shortLocalized

private val LocalDate.shortLocalized: String
    @Composable get() {
        val monthName = when (monthNumber) {
            1 -> stringResource(Res.string.date_month_short_jan)
            2 -> stringResource(Res.string.date_month_short_feb)
            3 -> stringResource(Res.string.date_month_short_mar)
            4 -> stringResource(Res.string.date_month_short_apr)
            5 -> stringResource(Res.string.date_month_short_may)
            6 -> stringResource(Res.string.date_month_short_jun)
            7 -> stringResource(Res.string.date_month_short_jul)
            8 -> stringResource(Res.string.date_month_short_aug)
            9 -> stringResource(Res.string.date_month_short_sep)
            10 -> stringResource(Res.string.date_month_short_oct)
            11 -> stringResource(Res.string.date_month_short_nov)
            12 -> stringResource(Res.string.date_month_short_dec)
            else -> "?"
        }
        return "$monthName $day"
    }
