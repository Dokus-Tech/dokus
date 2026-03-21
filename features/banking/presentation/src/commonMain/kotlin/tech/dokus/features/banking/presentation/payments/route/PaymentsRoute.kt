package tech.dokus.features.banking.presentation.payments.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_payments_subtitle
import tech.dokus.aura.resources.banking_payments_title
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.features.banking.presentation.payments.components.IGNORE_RESULT_KEY
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsContainer
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsIntent
import tech.dokus.features.banking.presentation.payments.screen.PaymentsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.navigation.destinations.BankingDestination
import tech.dokus.navigation.local.LocalNavController

private const val HOME_ROUTE_PAYMENTS = "payments"

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)

@Composable
internal fun PaymentsRoute(
    container: PaymentsContainer = container(),
) {
    val navController = LocalNavController.current
    val state by container.store.subscribe(DefaultLifecycle)

    // Observe ignore dialog result from navigation
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>(IGNORE_RESULT_KEY, null)
            ?.collect { result ->
                if (result != null) {
                    savedStateHandle.remove<String>(IGNORE_RESULT_KEY)
                    val parts = result.split(":", limit = 2)
                    if (parts.size == 2) {
                        val transactionId = parts[0]
                        val reason = IgnoredReason.valueOf(parts[1])
                        container.store.intent(
                            PaymentsIntent.ConfirmIgnoreWithResult(transactionId, reason)
                        )
                    }
                }
            }
    }

    val paymentsTitle = stringResource(Res.string.banking_payments_title)
    val paymentsSubtitle = stringResource(Res.string.banking_payments_subtitle)
    val onIntent = remember(container, navController) {
        { intent: PaymentsIntent ->
            when (intent) {
                is PaymentsIntent.IgnoreTransaction -> {
                    navController.navigate(
                        BankingDestination.IgnoreReasonDialog(
                            transactionId = intent.transactionId.value.toString()
                        )
                    )
                }
                else -> container.store.intent(intent)
            }
        }
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
