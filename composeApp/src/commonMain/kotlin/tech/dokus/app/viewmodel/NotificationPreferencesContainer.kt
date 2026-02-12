package tech.dokus.app.viewmodel

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.app.notifications.NotificationRemoteDataSource
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.platform.Logger

internal typealias NotificationPreferencesCtx = PipelineContext<
    NotificationPreferencesState,
    NotificationPreferencesIntent,
    NotificationPreferencesAction,
    >

internal class NotificationPreferencesContainer(
    private val notificationRemoteDataSource: NotificationRemoteDataSource
) : Container<NotificationPreferencesState, NotificationPreferencesIntent, NotificationPreferencesAction> {

    private val logger = Logger.forClass<NotificationPreferencesContainer>()

    override val store: Store<NotificationPreferencesState, NotificationPreferencesIntent, NotificationPreferencesAction> =
        store(NotificationPreferencesState.Loading) {
            init {
                intent(NotificationPreferencesIntent.Load)
            }

            reduce { intent ->
                when (intent) {
                    NotificationPreferencesIntent.Load -> handleLoad()
                    is NotificationPreferencesIntent.ToggleEmail -> {
                        handleToggleEmail(intent.type, intent.enabled)
                    }
                }
            }
        }

    private suspend fun NotificationPreferencesCtx.handleLoad() {
        updateState { NotificationPreferencesState.Loading }

        notificationRemoteDataSource.getPreferences().fold(
            onSuccess = { response ->
                updateState {
                    NotificationPreferencesState.Content(
                        preferences = response.preferences.associateBy { it.type }
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notification preferences" }
                updateState {
                    NotificationPreferencesState.Error(
                        exception = error.toDokusException("Failed to load notification preferences"),
                        retryHandler = { intent(NotificationPreferencesIntent.Load) }
                    )
                }
            }
        )
    }

    private suspend fun NotificationPreferencesCtx.handleToggleEmail(
        type: NotificationType,
        enabled: Boolean
    ) {
        var previousPreference = NotificationPreferencesState.Content(
            preferences = emptyMap()
        ).preferenceFor(type)

        withState<NotificationPreferencesState.Content, _> {
            previousPreference = preferenceFor(type)
            updateState {
                copy(
                    preferences = preferences + (type to previousPreference.copy(emailEnabled = enabled)),
                    updatingTypes = updatingTypes + type
                )
            }
        }

        notificationRemoteDataSource.updatePreference(type, enabled).fold(
            onSuccess = { updated ->
                withState<NotificationPreferencesState.Content, _> {
                    updateState {
                        copy(
                            preferences = preferences + (type to updated),
                            updatingTypes = updatingTypes - type
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to update notification preference for $type" }
                withState<NotificationPreferencesState.Content, _> {
                    updateState {
                        copy(
                            preferences = preferences + (type to previousPreference),
                            updatingTypes = updatingTypes - type
                        )
                    }
                }
                action(
                    NotificationPreferencesAction.ShowError(
                        error.toDokusException("Failed to update notification preference")
                    )
                )
            }
        )
    }

    private fun Throwable.toDokusException(defaultMessage: String): DokusException {
        return this as? DokusException ?: DokusException.InternalError(defaultMessage)
    }
}

