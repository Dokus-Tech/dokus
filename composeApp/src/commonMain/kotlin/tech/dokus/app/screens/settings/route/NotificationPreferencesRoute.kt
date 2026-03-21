package tech.dokus.app.screens.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.NotificationPreferencesScreen
import tech.dokus.app.viewmodel.NotificationPreferencesContainer
import tech.dokus.foundation.app.mvi.container

@Composable
internal fun NotificationPreferencesRoute(
    container: NotificationPreferencesContainer = container()
) {
    val state by container.store.subscribe(DefaultLifecycle) { _ -> }

    NotificationPreferencesScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
