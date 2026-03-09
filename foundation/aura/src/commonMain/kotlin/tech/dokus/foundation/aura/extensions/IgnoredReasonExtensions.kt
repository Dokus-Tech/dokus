package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_ignore_reason_bank_fee
import tech.dokus.aura.resources.banking_ignore_reason_duplicate
import tech.dokus.aura.resources.banking_ignore_reason_irrelevant
import tech.dokus.aura.resources.banking_ignore_reason_not_business
import tech.dokus.aura.resources.banking_ignore_reason_other
import tech.dokus.aura.resources.banking_ignore_reason_personal
import tech.dokus.domain.enums.IgnoredReason

val IgnoredReason.localized: String
    @Composable get() = when (this) {
        IgnoredReason.BankFee -> stringResource(Res.string.banking_ignore_reason_bank_fee)
        IgnoredReason.DuplicateImport -> stringResource(Res.string.banking_ignore_reason_duplicate)
        IgnoredReason.Personal -> stringResource(Res.string.banking_ignore_reason_personal)
        IgnoredReason.NotBusiness -> stringResource(Res.string.banking_ignore_reason_not_business)
        IgnoredReason.Irrelevant -> stringResource(Res.string.banking_ignore_reason_irrelevant)
        IgnoredReason.Other -> stringResource(Res.string.banking_ignore_reason_other)
    }
