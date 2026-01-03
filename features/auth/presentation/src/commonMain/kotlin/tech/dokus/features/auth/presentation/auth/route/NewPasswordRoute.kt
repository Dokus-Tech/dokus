package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.NewPasswordAction
import tech.dokus.features.auth.mvi.NewPasswordContainer
import tech.dokus.features.auth.presentation.auth.screen.NewPasswordScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun NewPasswordRoute(
    container: NewPasswordContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            NewPasswordAction.NavigateBack -> navController.navigateUp()
        }
    }

    NewPasswordScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
