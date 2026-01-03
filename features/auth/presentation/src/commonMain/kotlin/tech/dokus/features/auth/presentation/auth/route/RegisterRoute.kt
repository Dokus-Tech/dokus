package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.RegisterAction
import tech.dokus.features.auth.mvi.RegisterContainer
import tech.dokus.features.auth.presentation.auth.screen.RegisterScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.replace

@Composable
internal fun RegisterRoute(
    container: RegisterContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            RegisterAction.NavigateToHome -> navController.replace(CoreDestination.Home)
            RegisterAction.NavigateToWorkspaceSelect -> navController.replace(AuthDestination.WorkspaceSelect)
        }
    }

    RegisterScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onNavigateUp = { navController.navigateUp() },
        onNavigateToLogin = { navController.navigateTo(AuthDestination.Login) },
    )
}
