package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_status_ignored
import tech.dokus.aura.resources.banking_status_linked
import tech.dokus.aura.resources.banking_status_suggested
import tech.dokus.aura.resources.banking_status_unmatched
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusWarning

val BankTransactionStatus.localized: String
    @Composable get() = when (this) {
        BankTransactionStatus.Unmatched -> stringResource(Res.string.banking_status_unmatched)
        BankTransactionStatus.Suggested -> stringResource(Res.string.banking_status_suggested)
        BankTransactionStatus.Linked -> stringResource(Res.string.banking_status_linked)
        BankTransactionStatus.Ignored -> stringResource(Res.string.banking_status_ignored)
    }

val BankTransactionStatus.statusColor: Color
    @Composable get() = when (this) {
        BankTransactionStatus.Unmatched -> MaterialTheme.colorScheme.statusWarning
        BankTransactionStatus.Suggested -> MaterialTheme.colorScheme.error
        BankTransactionStatus.Linked -> MaterialTheme.colorScheme.statusConfirmed
        BankTransactionStatus.Ignored -> MaterialTheme.colorScheme.onSurfaceVariant
    }
