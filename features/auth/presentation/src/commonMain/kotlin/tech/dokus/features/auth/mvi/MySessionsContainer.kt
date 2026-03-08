package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.features.auth.usecases.ListSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeOtherSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeSessionUseCase
import tech.dokus.foundation.platform.Logger

private typealias MySessionsCtx = PipelineContext<MySessionsState, MySessionsIntent, MySessionsAction>

internal class MySessionsContainer(
    private val listSessionsUseCase: ListSessionsUseCase,
    private val revokeSessionUseCase: RevokeSessionUseCase,
    private val revokeOtherSessionsUseCase: RevokeOtherSessionsUseCase
) : Container<MySessionsState, MySessionsIntent, MySessionsAction> {

    private val logger = Logger.forClass<MySessionsContainer>()

    override val store: Store<MySessionsState, MySessionsIntent, MySessionsAction> =
        store(MySessionsState.Loading) {
            init { intent(MySessionsIntent.Load) }

            reduce { intent ->
                when (intent) {
                    MySessionsIntent.Load -> load()
                    is MySessionsIntent.RevokeSession -> revokeSession(intent)
                    MySessionsIntent.RevokeOthers -> revokeOthers()
                    MySessionsIntent.BackClicked -> action(MySessionsAction.NavigateBack)
                }
            }
        }

    private suspend fun MySessionsCtx.load() {
        listSessionsUseCase().fold(
            onSuccess = { sessions ->
                updateState {
                    MySessionsState.Loaded(
                        sessions = sessions.onlyActiveSessions()
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load sessions" }
                updateState {
                    MySessionsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(MySessionsIntent.Load) }
                    )
                }
            }
        )
    }

    private suspend fun MySessionsCtx.revokeSession(intent: MySessionsIntent.RevokeSession) {
        revokeSessionUseCase(intent.sessionId).fold(
            onSuccess = {
                action(MySessionsAction.ShowSessionRevoked)
                this.intent(MySessionsIntent.Load)
            },
            onFailure = { error ->
                val dokusError = error.asDokusException
                logger.e(error) { "Failed to revoke session: ${intent.sessionId}" }
                var keepLoadedState = false
                updateState {
                    when (this) {
                        is MySessionsState.Loaded -> {
                            keepLoadedState = true
                            this
                        }

                        else -> MySessionsState.Error(
                            exception = dokusError,
                            retryHandler = { this@revokeSession.intent(MySessionsIntent.Load) }
                        )
                    }
                }
                if (keepLoadedState) {
                    action(MySessionsAction.ShowError(dokusError))
                }
            }
        )
    }

    private suspend fun MySessionsCtx.revokeOthers() {
        updateState {
            when (this) {
                is MySessionsState.Loaded -> copy(isRevokingOthers = true)
                else -> this
            }
        }

        revokeOtherSessionsUseCase().fold(
            onSuccess = {
                action(MySessionsAction.ShowRevokeOthersSuccess)
                intent(MySessionsIntent.Load)
            },
            onFailure = { error ->
                val dokusError = error.asDokusException
                logger.e(error) { "Failed to revoke other sessions" }
                var keepLoadedState = false
                updateState {
                    when (this) {
                        is MySessionsState.Loaded -> {
                            keepLoadedState = true
                            copy(isRevokingOthers = false)
                        }

                        else -> MySessionsState.Error(
                            exception = dokusError,
                            retryHandler = { intent(MySessionsIntent.Load) }
                        )
                    }
                }
                if (keepLoadedState) {
                    action(MySessionsAction.ShowError(dokusError))
                }
            }
        )
    }
}

private fun List<SessionDto>.onlyActiveSessions(
    nowEpochSeconds: Long = kotlin.time.Clock.System.now().epochSeconds
): List<SessionDto> {
    return filter { session ->
        val expiresAt = session.expiresAt
        session.revokedAt == null &&
                (expiresAt == null || expiresAt > nowEpochSeconds)
    }.sortedWith(
        compareByDescending<SessionDto> { it.isCurrent }
            .thenByDescending { it.lastActivityAt ?: it.createdAt ?: 0L }
            .thenByDescending { it.createdAt ?: 0L }
    )
}
