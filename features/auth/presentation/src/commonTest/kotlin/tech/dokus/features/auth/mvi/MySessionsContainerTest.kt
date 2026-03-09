package tech.dokus.features.auth.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DeviceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.features.auth.usecases.ListSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeOtherSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeSessionUseCase
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class MySessionsContainerTest {

    @Test
    fun `load keeps only active sessions and sorts current session first`() = runTest {
        val now = Clock.System.now().epochSeconds
        val currentSession = session(
            id = "00000000-0000-0000-0000-000000000001",
            isCurrent = true,
            createdAt = now - 10
        )
        val activeOther = session(
            id = "00000000-0000-0000-0000-000000000002",
            createdAt = now - 20
        )
        val expired = session(
            id = "00000000-0000-0000-0000-000000000003",
            createdAt = now - 30,
            expiresAt = now - 1
        )
        val revoked = session(
            id = "00000000-0000-0000-0000-000000000004",
            createdAt = now - 40,
            revokedAt = now - 5
        )
        val container = MySessionsContainer(
            listSessionsUseCase = FakeListSessionsUseCase(
                Result.success(listOf(expired, activeOther, revoked, currentSession))
            ),
            revokeSessionUseCase = FakeRevokeSessionUseCase(Result.success(Unit)),
            revokeOtherSessionsUseCase = FakeRevokeOtherSessionsUseCase(Result.success(Unit))
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val state = states.value
            val sessions = assertIs<DokusState.Success<*>>(state.sessions)
            @Suppress("UNCHECKED_CAST")
            val sessionList = sessions.data as List<SessionDto>
            assertEquals(
                listOf(currentSession.id, activeOther.id),
                sessionList.map { it.id }
            )
        }
    }

    @Test
    fun `revoke others failure keeps loaded state and clears progress`() = runTest {
        val currentSession = session(
            id = "00000000-0000-0000-0000-000000000001",
            isCurrent = true
        )
        val expectedError = DokusException.InternalError("boom")
        val container = MySessionsContainer(
            listSessionsUseCase = FakeListSessionsUseCase(Result.success(listOf(currentSession))),
            revokeSessionUseCase = FakeRevokeSessionUseCase(Result.success(Unit)),
            revokeOtherSessionsUseCase = FakeRevokeOtherSessionsUseCase(Result.failure(expectedError))
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            emit(MySessionsIntent.RevokeOthers)
            advanceUntilIdle()

            val state = states.value
            val sessions = assertIs<DokusState.Success<*>>(state.sessions)
            @Suppress("UNCHECKED_CAST")
            val sessionList = sessions.data as List<SessionDto>
            assertEquals(listOf(currentSession.id), sessionList.map { it.id })
            assertFalse(state.isRevokingOthers)
        }
    }
}

private class FakeListSessionsUseCase(
    private val result: Result<List<SessionDto>>
) : ListSessionsUseCase {
    override suspend fun invoke(): Result<List<SessionDto>> = result
}

private class FakeRevokeSessionUseCase(
    private val result: Result<Unit>
) : RevokeSessionUseCase {
    override suspend fun invoke(sessionId: SessionId): Result<Unit> = result
}

private class FakeRevokeOtherSessionsUseCase(
    private val result: Result<Unit>
) : RevokeOtherSessionsUseCase {
    override suspend fun invoke(): Result<Unit> = result
}

private fun session(
    id: String,
    isCurrent: Boolean = false,
    createdAt: Long = Clock.System.now().epochSeconds,
    expiresAt: Long = Clock.System.now().epochSeconds + 60,
    revokedAt: Long? = null,
): SessionDto {
    return SessionDto(
        id = SessionId(id),
        deviceType = DeviceType.Desktop,
        userAgent = "Chrome on macOS",
        ipAddress = "192.168.1.1",
        createdAt = createdAt,
        expiresAt = expiresAt,
        lastActivityAt = createdAt,
        revokedAt = revokedAt,
        isCurrent = isCurrent,
    )
}
