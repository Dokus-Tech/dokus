package ai.dokus.foundation.domain.usecases

import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.domain.model.common.HealthCheck
import ai.dokus.foundation.domain.model.common.HealthStatus
import ai.dokus.foundation.domain.model.common.ServerStatus
import ai.dokus.foundation.domain.rpc.HealthRemoteService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen

class GetCombinedHealthStatusUseCase(
    private val authHealthRemoteService: HealthRemoteService,
    private val expenseHealthRemoteService: HealthRemoteService,
    private val invoicingHealthRemoteService: HealthRemoteService,
) {
    fun execute(): Flow<List<HealthStatus>> {
        val authFlow = resilientHealthFlow(
            source = authHealthRemoteService.getHealthFlow(),
            feature = Feature.Auth
        )

        val expenseFlow = resilientHealthFlow(
            source = expenseHealthRemoteService.getHealthFlow(),
            feature = Feature.Expense
        )

        val invoicingFlow = resilientHealthFlow(
            source = invoicingHealthRemoteService.getHealthFlow(),
            feature = Feature.Invoicing
        )

        return combine(
            authFlow,
            expenseFlow,
            invoicingFlow
        ) { authHealth, expenseHealth, invoicingHealth ->
            listOf(authHealth, expenseHealth, invoicingHealth).sortedBy { it.feature }
        }
    }

    // Resilient wrapper: emits CONNECTING, retries on recoverable/timeouts, emits DOWN on errors
    private fun resilientHealthFlow(
        source: Flow<HealthStatus>,
        feature: Feature
    ): Flow<HealthStatus> {
        return source
            .retryWhen { cause, attempt ->
                val isRecoverable =
                    cause is TimeoutCancellationException || cause.asDokusException.recoverable
                if (isRecoverable) {
                    val backoffStep = (attempt.coerceAtMost(6)).toInt()
                    val delayMs = 300L * (1 shl backoffStep)
                    delay(delayMs)
                }
                isRecoverable
            }
            .onStart {
                // Connecting
                emit(connectingStatus(feature))
            }
            .catch { cause ->
                // DOWN on failure
                emit(downStatus(feature, cause))
            }
    }

    private fun connectingStatus(feature: Feature): HealthStatus =
        HealthStatus(
            status = ServerStatus.UNKNOWN,
            checks = mapOf(
                "connection" to HealthCheck(
                    status = ServerStatus.UNKNOWN,
                    message = "Connecting..."
                )
            ),
            feature = feature
        )

    private fun downStatus(feature: Feature, cause: Throwable): HealthStatus {
        val dokusError = cause.asDokusException
        val message = dokusError.message
        return HealthStatus(
            status = ServerStatus.DOWN,
            checks = mapOf(
                "connection" to HealthCheck(
                    status = ServerStatus.DOWN,
                    message = message
                )
            ),
            feature = feature
        )
    }
}