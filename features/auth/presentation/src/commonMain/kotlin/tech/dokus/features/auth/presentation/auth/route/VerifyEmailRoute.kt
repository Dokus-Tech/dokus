package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.VerifyEmailAction
import tech.dokus.features.auth.mvi.VerifyEmailContainer
import tech.dokus.features.auth.mvi.VerifyEmailIntent
import tech.dokus.features.auth.presentation.auth.screen.VerifyEmailScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun VerifyEmailRoute(
    token: String,
    container: VerifyEmailContainer = container(
        key = "verify-email-$token"
    ) {
        parametersOf(token)
    }
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            VerifyEmailAction.NavigateToLogin -> navController.navigateTo(AuthDestination.Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    VerifyEmailScreen(
        state = state,
        onContinue = {
            navController.navigateTo(AuthDestination.Login) {
                popUpTo(0) { inclusive = true }
            }
        },
        onRetry = { container.store.intent(VerifyEmailIntent.Verify) }
    )
}
