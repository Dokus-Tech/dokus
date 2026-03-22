package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.MySessionsAction
import tech.dokus.features.auth.mvi.MySessionsContainer
import tech.dokus.features.auth.presentation.auth.screen.MySessionsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.local.LocalNavController

@Composable
fun MySessionsRoute() {
    val container: MySessionsContainer = container()
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            MySessionsAction.NavigateBack -> navController.navigateUp()
        }
    }

    MySessionsScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
