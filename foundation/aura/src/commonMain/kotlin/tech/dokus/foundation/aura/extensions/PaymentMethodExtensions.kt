package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.payment_method_bank_transfer
import tech.dokus.aura.resources.payment_method_cash
import tech.dokus.aura.resources.payment_method_check
import tech.dokus.aura.resources.payment_method_credit_card
import tech.dokus.aura.resources.payment_method_debit_card
import tech.dokus.aura.resources.payment_method_other
import tech.dokus.aura.resources.payment_method_paypal
import tech.dokus.aura.resources.payment_method_stripe
import tech.dokus.domain.enums.PaymentMethod

val PaymentMethod.localized: String
    @Composable get() = when (this) {
        PaymentMethod.BankTransfer -> stringResource(Res.string.payment_method_bank_transfer)
        PaymentMethod.CreditCard -> stringResource(Res.string.payment_method_credit_card)
        PaymentMethod.DebitCard -> stringResource(Res.string.payment_method_debit_card)
        PaymentMethod.PayPal -> stringResource(Res.string.payment_method_paypal)
        PaymentMethod.Stripe -> stringResource(Res.string.payment_method_stripe)
        PaymentMethod.Cash -> stringResource(Res.string.payment_method_cash)
        PaymentMethod.Check -> stringResource(Res.string.payment_method_check)
        PaymentMethod.Other -> stringResource(Res.string.payment_method_other)
    }
