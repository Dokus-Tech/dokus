package tech.dokus.features.banking.presentation.balances.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_subtitle
import tech.dokus.aura.resources.banking_balances_title
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.banking.presentation.balances.mvi.BalancesAction
import tech.dokus.features.banking.presentation.balances.mvi.BalancesContainer
import tech.dokus.features.banking.presentation.balances.mvi.BalancesIntent
import tech.dokus.features.banking.presentation.balances.screen.BalancesScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.foundation.aura.extensions.localized

private const val HOME_ROUTE_BALANCES = "balances"

@Composable
internal fun BalancesRoute(
    container: BalancesContainer = container(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is BalancesAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    val title = stringResource(Res.string.banking_balances_title)
    val subtitle = stringResource(Res.string.banking_balances_subtitle)
    val onIntent = remember(container) {
        { intent: BalancesIntent -> container.store.intent(intent) }
    }
    val topBarConfig = remember(title, subtitle) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = title,
                subtitle = subtitle,
            )
        )
    }

    RegisterHomeShellTopBar(
        route = HOME_ROUTE_BALANCES,
        config = topBarConfig,
    )

    BalancesScreen(
        state = state,
        onIntent = onIntent,
        snackbarHostState = snackbarHostState,
    )
}
