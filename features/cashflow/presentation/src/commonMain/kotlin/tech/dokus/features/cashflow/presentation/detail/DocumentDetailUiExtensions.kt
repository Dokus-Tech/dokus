package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_overdue_days_inline
import tech.dokus.aura.resources.document_overdue_with_days
import tech.dokus.aura.resources.document_payment_awaiting
import tech.dokus.aura.resources.document_payment_due_on
import tech.dokus.aura.resources.document_payment_paid_on
import tech.dokus.aura.resources.document_payment_recorded
import tech.dokus.aura.resources.payment_method_bank_transfer
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate

val DocumentDetailState.statusBadgeLocalized: String
    @Composable get() = when (financialStatus) {
        ReviewFinancialStatus.Overdue -> overdueDays?.let { days ->
            stringResource(Res.string.document_overdue_with_days, days)
        } ?: financialStatus.localized
        else -> financialStatus.localized
    }

val DocumentDetailState.compressedStatusDetailLocalized: String
    @Composable get() = when (financialStatus) {
        ReviewFinancialStatus.Paid -> paidAtDate?.let { paidAt ->
            stringResource(Res.string.document_payment_paid_on, formatShortDate(paidAt))
        } ?: stringResource(Res.string.document_payment_recorded)
        ReviewFinancialStatus.Overdue -> overdueDays?.let { days ->
            stringResource(Res.string.document_overdue_with_days, days)
        } ?: financialStatus.localized
        ReviewFinancialStatus.Unpaid -> stringResource(Res.string.document_payment_awaiting)
        ReviewFinancialStatus.Review -> financialStatus.localized
    }

val DocumentDetailState.paidHeadlineLocalized: String
    @Composable get() = paidAtDate?.let { paidAt ->
        stringResource(Res.string.document_payment_paid_on, formatShortDate(paidAt))
    } ?: stringResource(Res.string.document_payment_recorded)

val DocumentDetailState.paidMethodLocalized: String
    @Composable get() = stringResource(Res.string.payment_method_bank_transfer)

val DocumentDetailState.paymentDueLocalized: String?
    @Composable get() = resolvedDueDate?.let { dueDate ->
        stringResource(Res.string.document_payment_due_on, formatShortDate(dueDate))
    }

val DocumentDetailState.overdueInlineLocalized: String?
    @Composable get() = overdueDays?.let { days ->
        stringResource(Res.string.document_overdue_days_inline, days)
    }
