package tech.dokus.features.cashflow.presentation.review

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_detail_needs_review
import tech.dokus.aura.resources.payable_invoice_status_overdue
import tech.dokus.aura.resources.payable_invoice_status_paid
import tech.dokus.aura.resources.payable_invoice_status_pending
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning

val ReviewFinancialStatus.localized: String
    @Composable get() = when (this) {
        ReviewFinancialStatus.Paid -> stringResource(Res.string.payable_invoice_status_paid)
        ReviewFinancialStatus.Unpaid -> stringResource(Res.string.payable_invoice_status_pending)
        ReviewFinancialStatus.Overdue -> stringResource(Res.string.payable_invoice_status_overdue)
        ReviewFinancialStatus.Review -> stringResource(Res.string.document_detail_needs_review)
    }

val ReviewFinancialStatus.colorized: Color
    @Composable get() = when (this) {
        ReviewFinancialStatus.Paid -> MaterialTheme.colorScheme.statusConfirmed
        ReviewFinancialStatus.Unpaid -> MaterialTheme.colorScheme.statusWarning
        ReviewFinancialStatus.Overdue -> MaterialTheme.colorScheme.statusError
        ReviewFinancialStatus.Review -> MaterialTheme.colorScheme.statusWarning
    }

val ReviewFinancialStatus.dotType: StatusDotType
    get() = when (this) {
        ReviewFinancialStatus.Paid -> StatusDotType.Confirmed
        ReviewFinancialStatus.Unpaid -> StatusDotType.Warning
        ReviewFinancialStatus.Overdue -> StatusDotType.Error
        ReviewFinancialStatus.Review -> StatusDotType.Warning
    }
