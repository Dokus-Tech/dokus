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
import tech.dokus.aura.resources.profile_session_revoked
import tech.dokus.aura.resources.profile_sessions_revoke_others_success
import tech.dokus.features.auth.mvi.MySessionsAction
import tech.dokus.features.auth.mvi.MySessionsContainer
import tech.dokus.features.auth.presentation.auth.screen.MySessionsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
fun MySessionsRoute() {
    val container: MySessionsContainer = container()
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingMessage by remember { mutableStateOf<String?>(null) }
    val sessionRevokedMessage = stringResource(Res.string.profile_session_revoked)
    val revokeOthersMessage = stringResource(Res.string.profile_sessions_revoke_others_success)

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            MySessionsAction.NavigateBack -> navController.navigateUp()
            MySessionsAction.ShowSessionRevoked -> pendingMessage = sessionRevokedMessage
            MySessionsAction.ShowRevokeOthersSuccess -> pendingMessage = revokeOthersMessage
        }
    }

    LaunchedEffect(pendingMessage) {
        pendingMessage?.let {
            snackbarHostState.showSnackbar(it)
            pendingMessage = null
        }
    }

    MySessionsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) }
    )
}
