package tech.dokus.features.cashflow.mvi

import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.model.PeppolProvider

internal typealias PeppolSettingsCtx = PipelineContext<PeppolSettingsState, PeppolSettingsIntent, PeppolSettingsAction>

/**
 * Container for Peppol settings management using FlowMVI.
 * Handles loading settings, provider selection, and disconnection.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
class PeppolSettingsContainer(
    private val dataSource: CashflowRemoteDataSource,
) : Container<PeppolSettingsState, PeppolSettingsIntent, PeppolSettingsAction> {

    private val logger = Logger.forClass<PeppolSettingsContainer>()

    override val store: Store<PeppolSettingsState, PeppolSettingsIntent, PeppolSettingsAction> =
        store(PeppolSettingsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is PeppolSettingsIntent.LoadSettings -> handleLoadSettings()
                    is PeppolSettingsIntent.SelectProvider -> handleSelectProvider(intent.provider)
                    is PeppolSettingsIntent.DeleteSettingsClicked -> handleDeleteClicked()
                    is PeppolSettingsIntent.ConfirmDelete -> handleConfirmDelete()
                    is PeppolSettingsIntent.CancelDelete -> handleCancelDelete()
                }
            }
        }

    private suspend fun PeppolSettingsCtx.handleLoadSettings() {
        updateState { PeppolSettingsState.Loading }

        logger.d { "Loading Peppol settings" }
        dataSource.getPeppolSettings().fold(
            onSuccess = { settings ->
                logger.i { "Peppol settings loaded: ${if (settings != null) "configured" else "not configured"}" }
                updateState {
                    if (settings != null) {
                        PeppolSettingsState.Connected(settings = settings)
                    } else {
                        PeppolSettingsState.NotConfigured
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load Peppol settings" }
                updateState {
                    PeppolSettingsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(PeppolSettingsIntent.LoadSettings) }
                    )
                }
            }
        )
    }

    private suspend fun PeppolSettingsCtx.handleSelectProvider(
        provider: PeppolProvider
    ) {
        action(PeppolSettingsAction.NavigateToPeppolConnect(provider))
    }

    private suspend fun PeppolSettingsCtx.handleDeleteClicked() {
        withState<PeppolSettingsState.Connected, _> {
            action(PeppolSettingsAction.ShowDeleteConfirmation)
        }
    }

    private suspend fun PeppolSettingsCtx.handleConfirmDelete() {
        withState<PeppolSettingsState.Connected, _> {
            val currentSettings = settings

            updateState {
                PeppolSettingsState.Deleting(settings = currentSettings)
            }

            logger.d { "Deleting Peppol settings" }
            dataSource.deletePeppolSettings().fold(
                onSuccess = {
                    logger.i { "Peppol settings deleted" }
                    action(PeppolSettingsAction.ShowDeleteSuccess)
                    updateState { PeppolSettingsState.NotConfigured }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete Peppol settings" }
                    updateState {
                        PeppolSettingsState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(PeppolSettingsIntent.ConfirmDelete) }
                        )
                    }
                }
            )
        }
    }

    @Suppress("UnusedReceiverParameter")
    private suspend fun PeppolSettingsCtx.handleCancelDelete() {
        // Dialog is dismissed by the UI, no state change needed
        // The state remains in Connected while the confirmation dialog is shown
    }
}
