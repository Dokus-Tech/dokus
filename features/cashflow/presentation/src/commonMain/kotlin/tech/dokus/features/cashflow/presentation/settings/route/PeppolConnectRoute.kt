package tech.dokus.features.cashflow.presentation.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.features.cashflow.mvi.PeppolConnectAction
import tech.dokus.features.cashflow.mvi.PeppolConnectContainer
import tech.dokus.features.cashflow.presentation.settings.screen.PeppolConnectScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
fun PeppolConnectRoute(
    provider: PeppolProvider
) {
    PeppolConnectRouteInternal(provider = provider)
}

@Composable
internal fun PeppolConnectRouteInternal(
    provider: PeppolProvider,
    container: PeppolConnectContainer = container {
        parametersOf(PeppolConnectContainer.Companion.Params(provider))
    }
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            PeppolConnectAction.NavigateBack -> navController.navigateUp()
            PeppolConnectAction.NavigateToSettings -> {
                navController.navigateTo(SettingsDestination.PeppolSettings) {
                    popUpTo(SettingsDestination.PeppolSettings) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    PeppolConnectScreen(
        provider = provider,
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
