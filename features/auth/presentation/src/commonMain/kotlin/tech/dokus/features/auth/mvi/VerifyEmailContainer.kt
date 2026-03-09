package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.features.auth.usecases.VerifyEmailUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

private typealias VerifyEmailCtx = PipelineContext<VerifyEmailState, VerifyEmailIntent, VerifyEmailAction>

internal class VerifyEmailContainer(
    private val token: String,
    private val verifyEmailUseCase: VerifyEmailUseCase
) : Container<VerifyEmailState, VerifyEmailIntent, VerifyEmailAction> {

    private val logger = Logger.forClass<VerifyEmailContainer>()

    override val store: Store<VerifyEmailState, VerifyEmailIntent, VerifyEmailAction> =
        store(VerifyEmailState.initial) {
            init { intent(VerifyEmailIntent.Verify) }

            reduce { intent ->
                when (intent) {
                    VerifyEmailIntent.Verify -> handleVerify()
                }
            }
        }

    private suspend fun VerifyEmailCtx.handleVerify() {
        updateState { copy(verification = verification.asLoading) }
        verifyEmailUseCase(token).fold(
            onSuccess = {
                logger.i { "Email verification succeeded" }
                updateState { copy(verification = DokusState.success(Unit)) }
            },
            onFailure = { error ->
                logger.e(error) { "Email verification failed" }
                updateState {
                    copy(
                        verification = DokusState.error(
                            exception = error,
                            retryHandler = { intent(VerifyEmailIntent.Verify) }
                        )
                    )
                }
            }
        )
    }
}
