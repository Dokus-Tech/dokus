package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_account_status_confirmed
import tech.dokus.aura.resources.banking_account_status_pending
import tech.dokus.domain.enums.BankAccountStatus

val BankAccountStatus.localized: String
    @Composable get() = when (this) {
        BankAccountStatus.Confirmed -> stringResource(Res.string.banking_account_status_confirmed)
        BankAccountStatus.PendingReview -> stringResource(Res.string.banking_account_status_pending)
    }
