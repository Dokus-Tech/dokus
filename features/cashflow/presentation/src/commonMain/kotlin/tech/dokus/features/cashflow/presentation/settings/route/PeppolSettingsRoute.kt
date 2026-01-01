package tech.dokus.features.cashflow.presentation.settings.route

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
import tech.dokus.aura.resources.peppol_delete_success
import tech.dokus.features.cashflow.mvi.PeppolSettingsAction
import tech.dokus.features.cashflow.mvi.PeppolSettingsContainer
import tech.dokus.features.cashflow.mvi.PeppolSettingsIntent
import tech.dokus.features.cashflow.presentation.settings.screen.PeppolSettingsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
fun PeppolSettingsRoute(
    container: PeppolSettingsContainer = container()
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val successMessage = if (pendingSuccess) {
        stringResource(Res.string.peppol_delete_success)
    } else {
        null
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = false
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is PeppolSettingsAction.NavigateToPeppolConnect -> {
                navController.navigateTo(
                    SettingsDestination.PeppolConfiguration.Connect(action.provider.name)
                )
            }
            PeppolSettingsAction.NavigateBack -> navController.navigateUp()
            PeppolSettingsAction.ShowDeleteConfirmation -> {
                showDeleteConfirmation = true
            }
            PeppolSettingsAction.ShowDeleteSuccess -> {
                pendingSuccess = true
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(PeppolSettingsIntent.LoadSettings)
    }

    PeppolSettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        showDeleteConfirmation = showDeleteConfirmation,
        onIntent = { container.store.intent(it) },
        onDeleteDismiss = {
            showDeleteConfirmation = false
            container.store.intent(PeppolSettingsIntent.CancelDelete)
        },
        onDeleteConfirm = {
            showDeleteConfirmation = false
            container.store.intent(PeppolSettingsIntent.ConfirmDelete)
        }
    )
}
