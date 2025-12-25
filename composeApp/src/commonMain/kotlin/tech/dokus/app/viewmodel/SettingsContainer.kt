package tech.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

internal typealias SettingsCtx = PipelineContext<SettingsState, SettingsIntent, SettingsAction>

/**
 * Container for Settings screen using FlowMVI.
 *
 * Manages the current tenant/workspace state for the workspace picker
 * and settings navigation.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class SettingsContainer(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
) : Container<SettingsState, SettingsIntent, SettingsAction> {

    private val logger = Logger.forClass<SettingsContainer>()

    override val store: Store<SettingsState, SettingsIntent, SettingsAction> =
        store(SettingsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is SettingsIntent.Load -> handleLoad()
                    is SettingsIntent.Refresh -> handleLoad()
                }
            }
        }

    private suspend fun SettingsCtx.handleLoad() {
        logger.d { "Loading current tenant" }

        updateState { SettingsState.Loading }

        getCurrentTenantUseCase().fold(
            onSuccess = { tenant ->
                logger.i { "Current tenant loaded: ${tenant?.displayName?.value}" }
                updateState { SettingsState.Content(tenant = tenant) }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load current tenant" }
                updateState {
                    SettingsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(SettingsIntent.Load) }
                    )
                }
            }
        )
    }
}
