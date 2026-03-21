package tech.dokus.features.banking.presentation.payments.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_payments_subtitle
import tech.dokus.aura.resources.banking_payments_title
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsContainer
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsIntent
import tech.dokus.features.banking.presentation.payments.screen.PaymentsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar

private const val HOME_ROUTE_PAYMENTS = "payments"

@Composable
internal fun PaymentsRoute(
    container: PaymentsContainer = container(),
) {
    val state by container.store.subscribe(DefaultLifecycle)

    val paymentsTitle = stringResource(Res.string.banking_payments_title)
    val paymentsSubtitle = stringResource(Res.string.banking_payments_subtitle)
    val onIntent = remember(container) {
        { intent: PaymentsIntent -> container.store.intent(intent) }
    }
    val topBarConfig = remember(paymentsTitle, paymentsSubtitle) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = paymentsTitle,
                subtitle = paymentsSubtitle,
            )
        )
    }

    RegisterHomeShellTopBar(
        route = HOME_ROUTE_PAYMENTS,
        config = topBarConfig,
    )

    PaymentsScreen(
        state = state,
        onIntent = onIntent,
    )
}
