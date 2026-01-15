package tech.dokus.features.cashflow.presentation.peppol.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationAction
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationContainer
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.screen.PeppolRegistrationScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun PeppolRegistrationRoute(
    container: PeppolRegistrationContainer = container()
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var pendingSuccess by remember { mutableStateOf<String?>(null) }

    // Show error messages
    val errorMessage = pendingError?.localized
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    // Show success messages
    LaunchedEffect(pendingSuccess) {
        pendingSuccess?.let {
            snackbarHostState.showSnackbar(it)
            pendingSuccess = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is PeppolRegistrationAction.ShowError -> pendingError = action.error
            is PeppolRegistrationAction.ShowSuccess -> pendingSuccess = action.message
            is PeppolRegistrationAction.NavigateBack -> navController.popBackStack()
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    // Load registration status on first composition
    LaunchedEffect(Unit) {
        container.store.intent(PeppolRegistrationIntent.Refresh)
    }

    PeppolRegistrationScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}
