package tech.dokus.app.viewmodel

import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias HomeCtx = PipelineContext<HomeState, HomeIntent, HomeAction>

/**
 * Container for Home screen using FlowMVI.
 *
 * Manages the main navigation shell state. This is a lightweight container
 * since the Home screen is primarily a navigation container for:
 * - Bottom navigation (mobile)
 * - Navigation rail (desktop/tablet)
 * - Nested navigation host
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class HomeContainer : Container<HomeState, HomeIntent, HomeAction> {

    private val logger = Logger.forClass<HomeContainer>()

    override val store: Store<HomeState, HomeIntent, HomeAction> =
        store(HomeState.Ready) {
            reduce { intent ->
                when (intent) {
                    is HomeIntent.ScreenAppeared -> handleScreenAppeared()
                }
            }
        }

    @Suppress("UnusedReceiverParameter")
    private fun HomeCtx.handleScreenAppeared() {
        logger.d { "Home screen appeared" }
        // No initialization needed - home screen is a navigation shell
    }
}
