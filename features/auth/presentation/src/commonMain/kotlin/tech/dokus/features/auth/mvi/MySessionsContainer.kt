package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.features.auth.usecases.ListSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeOtherSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeSessionUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger
import kotlin.time.Clock

private typealias MySessionsCtx = PipelineContext<MySessionsState, MySessionsIntent, MySessionsAction>

internal class MySessionsContainer(
    private val listSessionsUseCase: ListSessionsUseCase,
    private val revokeSessionUseCase: RevokeSessionUseCase,
    private val revokeOtherSessionsUseCase: RevokeOtherSessionsUseCase
) : Container<MySessionsState, MySessionsIntent, MySessionsAction> {

    private val logger = Logger.forClass<MySessionsContainer>()

    override val store: Store<MySessionsState, MySessionsIntent, MySessionsAction> =
        store(MySessionsState.initial) {
            init { intent(MySessionsIntent.Load) }

            reduce { intent ->
                when (intent) {
                    MySessionsIntent.Load -> load()
                    is MySessionsIntent.RevokeSession -> revokeSession(intent)
                    MySessionsIntent.RevokeOthers -> revokeOthers()
                    MySessionsIntent.DismissActionError -> updateState { copy(actionError = null) }
                    MySessionsIntent.BackClicked -> action(MySessionsAction.NavigateBack)
                }
            }
        }

    private suspend fun MySessionsCtx.load() {
        listSessionsUseCase().fold(
            onSuccess = { sessions ->
                updateState {
                    copy(sessions = DokusState.success(sessions.onlyActiveSessions()))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load sessions" }
                updateState {
                    copy(sessions = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(MySessionsIntent.Load) }
                    ))
                }
            }
        )
    }

    private suspend fun MySessionsCtx.revokeSession(intent: MySessionsIntent.RevokeSession) {
        revokeSessionUseCase(intent.sessionId).fold(
            onSuccess = {
                updateState { copy(actionError = null) }
                this.intent(MySessionsIntent.Load)
            },
            onFailure = { error ->
                val dokusError = error.asDokusException
                logger.e(error) { "Failed to revoke session: ${intent.sessionId}" }
                withState {
                    if (sessions.isSuccess()) {
                        updateState { copy(actionError = dokusError) }
                    } else {
                        updateState {
                            copy(sessions = DokusState.error(
                                exception = dokusError,
                                retryHandler = { this@revokeSession.intent(MySessionsIntent.Load) }
                            ))
                        }
                    }
                }
            }
        )
    }

    private suspend fun MySessionsCtx.revokeOthers() {
        updateState { copy(isRevokingOthers = true) }

        revokeOtherSessionsUseCase().fold(
            onSuccess = {
                updateState { copy(actionError = null) }
                intent(MySessionsIntent.Load)
            },
            onFailure = { error ->
                val dokusError = error.asDokusException
                logger.e(error) { "Failed to revoke other sessions" }
                updateState { copy(isRevokingOthers = false) }
                withState {
                    if (sessions.isSuccess()) {
                        updateState { copy(actionError = dokusError) }
                    } else {
                        updateState {
                            copy(sessions = DokusState.error(
                                exception = dokusError,
                                retryHandler = { intent(MySessionsIntent.Load) }
                            ))
                        }
                    }
                }
            }
        )
    }
}

private fun List<SessionDto>.onlyActiveSessions(
    nowEpochSeconds: Long = Clock.System.now().epochSeconds
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
