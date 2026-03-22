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
import tech.dokus.foundation.app.state.DokusState
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
        store(NotificationPreferencesState.initial) {
            init {
                intent(NotificationPreferencesIntent.Load)
            }

            reduce { intent ->
                when (intent) {
                    NotificationPreferencesIntent.Load -> handleLoad()
                    is NotificationPreferencesIntent.ToggleEmail -> {
                        handleToggleEmail(intent.type, intent.enabled)
                    }
                    is NotificationPreferencesIntent.DismissActionError -> updateState { copy(actionError = null) }
                }
            }
        }

    private suspend fun NotificationPreferencesCtx.handleLoad() {
        updateState { copy(preferences = preferences.asLoading) }

        notificationRemoteDataSource.getPreferences().fold(
            onSuccess = { response ->
                updateState {
                    copy(preferences = DokusState.success(response.preferences.associateBy { it.type }))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load notification preferences" }
                updateState {
                    copy(preferences = DokusState.error(
                        exception = error.toDokusException("Failed to load notification preferences"),
                        retryHandler = { intent(NotificationPreferencesIntent.Load) }
                    ))
                }
            }
        )
    }

    private suspend fun NotificationPreferencesCtx.handleToggleEmail(
        type: NotificationType,
        enabled: Boolean
    ) {
        var previousPreference = NotificationPreferencesState().preferenceFor(type)

        withState {
            val prefsMap = (preferences as? DokusState.Success)?.data ?: return@withState
            previousPreference = preferenceFor(type)
            updateState {
                copy(
                    preferences = DokusState.success(prefsMap + (type to previousPreference.copy(emailEnabled = enabled))),
                    updatingTypes = updatingTypes + type
                )
            }
        }

        notificationRemoteDataSource.updatePreference(type, enabled).fold(
            onSuccess = { updated ->
                withState {
                    val prefsMap = (preferences as? DokusState.Success)?.data ?: return@withState
                    updateState {
                        copy(
                            preferences = DokusState.success(prefsMap + (type to updated)),
                            updatingTypes = updatingTypes - type
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to update notification preference for $type" }
                withState {
                    val prefsMap = (preferences as? DokusState.Success)?.data ?: return@withState
                    updateState {
                        copy(
                            preferences = DokusState.success(prefsMap + (type to previousPreference)),
                            updatingTypes = updatingTypes - type
                        )
                    }
                }
                updateState {
                    copy(actionError = error.toDokusException("Failed to update notification preference"))
                }
            }
        )
    }

    private fun Throwable.toDokusException(defaultMessage: String): DokusException {
        return this as? DokusException ?: DokusException.InternalError(defaultMessage)
    }
}

