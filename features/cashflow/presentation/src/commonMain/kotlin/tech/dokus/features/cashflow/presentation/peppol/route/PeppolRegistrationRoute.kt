package tech.dokus.features.cashflow.presentation.peppol.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationAction
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationContainer
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationPhase
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.features.cashflow.presentation.peppol.screen.PeppolRegistrationScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun PeppolRegistrationRoute(
    container: PeppolRegistrationContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is PeppolRegistrationAction.NavigateToHome -> navController.replace(CoreDestination.Home)
        }
    }

    // Load registration status on first composition
    LaunchedEffect(Unit) {
        container.store.intent(PeppolRegistrationIntent.Refresh)
    }

    // Poll for transfer completion while the user is on the waiting screen.
    val isWaitingTransfer = state.setupContext.isSuccess() &&
        state.phase == PeppolRegistrationPhase.WaitingTransfer
    LaunchedEffect(isWaitingTransfer) {
        if (!isWaitingTransfer) return@LaunchedEffect
        while (true) {
            delay(30.seconds)
            container.store.intent(PeppolRegistrationIntent.PollTransfer)
        }
    }

    PeppolRegistrationScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
