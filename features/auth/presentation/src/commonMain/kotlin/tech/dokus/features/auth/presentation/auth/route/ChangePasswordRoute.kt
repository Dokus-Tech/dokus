package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.ChangePasswordAction
import tech.dokus.features.auth.mvi.ChangePasswordContainer
import tech.dokus.features.auth.presentation.auth.screen.ChangePasswordScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
fun ChangePasswordRoute() {
    val container: ChangePasswordContainer = container()
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ChangePasswordAction.NavigateBack -> navController.navigateUp()
        }
    }

    ChangePasswordScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
