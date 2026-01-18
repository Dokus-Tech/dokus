package tech.dokus.features.cashflow.presentation.peppol.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationAction
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationContainer
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
import tech.dokus.features.cashflow.presentation.peppol.screen.PeppolRegistrationScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

@Composable
internal fun PeppolRegistrationRoute(
    container: PeppolRegistrationContainer = container()
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    // Show error messages
    val errorMessage = pendingError?.localized
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is PeppolRegistrationAction.ShowError -> pendingError = action.error
            is PeppolRegistrationAction.NavigateToHome -> navController.replace(CoreDestination.Home)
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    // Load registration status on first composition
    LaunchedEffect(Unit) {
        container.store.intent(PeppolRegistrationIntent.Refresh)
    }

    // Poll for transfer completion while the user is on the waiting screen.
    LaunchedEffect(state) {
        if (state !is PeppolRegistrationState.WaitingTransfer) return@LaunchedEffect
        while (true) {
            delay(30_000)
            container.store.intent(PeppolRegistrationIntent.PollTransfer)
        }
    }

    PeppolRegistrationScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}
