package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.NewPasswordAction
import tech.dokus.features.auth.mvi.NewPasswordContainer
import tech.dokus.features.auth.presentation.auth.screen.NewPasswordScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun NewPasswordRoute(
    token: String,
    container: NewPasswordContainer = container(
        key = "new-password-$token"
    ) {
        parametersOf(token)
    }
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            NewPasswordAction.NavigateBack -> navController.navigateTo(AuthDestination.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NewPasswordScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
