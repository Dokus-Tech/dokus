package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_status_ignored
import tech.dokus.aura.resources.banking_status_matched
import tech.dokus.aura.resources.banking_status_needs_review
import tech.dokus.aura.resources.banking_status_unmatched
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusWarning

val BankTransactionStatus.localized: String
    @Composable get() = when (this) {
        BankTransactionStatus.Unmatched -> stringResource(Res.string.banking_status_unmatched)
        BankTransactionStatus.NeedsReview -> stringResource(Res.string.banking_status_needs_review)
        BankTransactionStatus.Matched -> stringResource(Res.string.banking_status_matched)
        BankTransactionStatus.Ignored -> stringResource(Res.string.banking_status_ignored)
    }

val BankTransactionStatus.statusColor: Color
    @Composable get() = when (this) {
        BankTransactionStatus.Unmatched -> MaterialTheme.colorScheme.statusWarning
        BankTransactionStatus.NeedsReview -> MaterialTheme.colorScheme.error
        BankTransactionStatus.Matched -> MaterialTheme.colorScheme.statusConfirmed
        BankTransactionStatus.Ignored -> MaterialTheme.colorScheme.onSurfaceVariant
    }
