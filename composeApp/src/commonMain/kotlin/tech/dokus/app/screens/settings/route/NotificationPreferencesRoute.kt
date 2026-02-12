package tech.dokus.app.screens.settings.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.NotificationPreferencesScreen
import tech.dokus.app.viewmodel.NotificationPreferencesAction
import tech.dokus.app.viewmodel.NotificationPreferencesContainer
import tech.dokus.app.viewmodel.NotificationPreferencesIntent
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun NotificationPreferencesRoute(
    container: NotificationPreferencesContainer = container()
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
            is NotificationPreferencesAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    NotificationPreferencesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}

