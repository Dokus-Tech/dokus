package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.LoginAction
import tech.dokus.features.auth.mvi.LoginContainer
import tech.dokus.features.auth.presentation.auth.screen.LoginScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.replace

@Composable
internal fun LoginRoute(
    container: LoginContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            LoginAction.NavigateToHome -> navController.replace(CoreDestination.Home)
            LoginAction.NavigateToWorkspaceSelect -> navController.replace(AuthDestination.WorkspaceSelect)
        }
    }

    LoginScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onForgotPassword = { navController.navigateTo(AuthDestination.ForgotPassword) },
        onConnectToServer = { navController.navigateTo(AuthDestination.ServerConnection()) },
        onRegister = { navController.navigateTo(AuthDestination.Register) },
    )
}
