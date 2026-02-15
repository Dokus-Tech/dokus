package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
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
                updateState { MySessionsState.Loaded(sessions = sessions) }
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
                logger.e(error) { "Failed to revoke session: ${intent.sessionId}" }
                updateState {
                    MySessionsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { this@revokeSession.intent(MySessionsIntent.Load) }
                    )
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
                logger.e(error) { "Failed to revoke other sessions" }
                updateState {
                    MySessionsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(MySessionsIntent.Load) }
                    )
                }
            }
        )
    }
}
