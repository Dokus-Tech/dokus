package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.ForgotPasswordAction
import tech.dokus.features.auth.mvi.ForgotPasswordContainer
import tech.dokus.features.auth.presentation.auth.screen.ForgotPasswordScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
internal fun ForgotPasswordRoute(
    container: ForgotPasswordContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ForgotPasswordAction.NavigateBack -> navController.navigateUp()
        }
    }

    ForgotPasswordScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
