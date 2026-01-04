package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.features.auth.mvi.ServerConnectionAction
import tech.dokus.features.auth.mvi.ServerConnectionContainer
import tech.dokus.features.auth.mvi.ServerConnectionIntent
import tech.dokus.features.auth.presentation.auth.screen.ServerConnectionScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

@Composable
internal fun ServerConnectionRoute(
    host: String? = null,
    port: Int? = null,
    protocol: String? = null,
    container: ServerConnectionContainer = container {
        val initialConfig = if (host != null && port != null) {
            ServerConfig.fromManualEntry(host, port, protocol ?: "http")
        } else {
            null
        }
        parametersOf(ServerConnectionContainer.Companion.Params(initialConfig))
    },
) {
    val navController = LocalNavController.current
    val serverConfigManager: ServerConfigManager = koinInject()
    val currentServer by serverConfigManager.currentServer.collectAsState()

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ServerConnectionAction.NavigateToLogin -> navController.replace(AuthDestination.Login)
            ServerConnectionAction.NavigateBack -> navController.popBackStack()
        }
    }

    LaunchedEffect(host, port) {
        if (host != null && port != null) {
            container.store.intent(ServerConnectionIntent.ValidateClicked)
        }
    }

    ServerConnectionScreen(
        state = state,
        currentServer = currentServer,
        onIntent = { container.store.intent(it) },
    )
}
