package tech.dokus.features.auth.presentation.auth.route

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
import tech.dokus.aura.resources.profile_password_changed
import tech.dokus.features.auth.mvi.ChangePasswordAction
import tech.dokus.features.auth.mvi.ChangePasswordContainer
import tech.dokus.features.auth.presentation.auth.screen.ChangePasswordScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
fun ChangePasswordRoute() {
    val container: ChangePasswordContainer = container()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf(false) }
    val successMessage = stringResource(Res.string.profile_password_changed)

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ChangePasswordAction.NavigateBack -> navController.navigateUp()
            ChangePasswordAction.ShowSuccess -> pendingSuccess = true
        }
    }

    LaunchedEffect(pendingSuccess) {
        if (pendingSuccess) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = false
        }
    }

    ChangePasswordScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}
