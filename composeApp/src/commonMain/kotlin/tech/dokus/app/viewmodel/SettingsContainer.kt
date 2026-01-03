package tech.dokus.app.viewmodel

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.foundation.platform.Logger

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
                if (tenant != null) {
                    updateState { SettingsState.Content(data = tenant) }
                    return@fold
                }
                updateState {
                    SettingsState.Error(
                        exception = DokusException.NotFound(),
                        retryHandler = { intent(SettingsIntent.Load) }
                    )
                }
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
