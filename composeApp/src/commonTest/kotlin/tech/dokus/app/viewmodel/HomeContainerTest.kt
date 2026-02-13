package tech.dokus.app.viewmodel

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.User
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeContainerTest {

    @Test
    fun `screen appeared loads tenant and user`() = runTest {
        val tenant = sampleTenant()
        val user = sampleUser()
        val tenantUseCase = FakeGetCurrentTenantUseCase(Result.success(tenant))
        val userUseCase = FakeGetCurrentUserUseCase(Result.success(user))
        val logoutUseCase = FakeLogoutUseCase()
        val container = HomeContainer(
            getCurrentTenantUseCase = tenantUseCase,
            getCurrentUserUseCase = userUseCase,
            logoutUseCase = logoutUseCase
        )

        container.store.subscribeAndTest {
            emit(HomeIntent.ScreenAppeared)
            advanceUntilIdle()

            val ready = assertIs<HomeState.Ready>(states.value)
            val tenantState = assertIs<DokusState.Success<Tenant?>>(ready.tenantState)
            val userState = assertIs<DokusState.Success<User>>(ready.userState)
            assertEquals(tenant, tenantState.data)
            assertEquals(user, userState.data)
            assertEquals(1, tenantUseCase.invocations)
            assertEquals(1, userUseCase.invocations)
        }
    }

    @Test
    fun `screen appeared handles no tenant selected`() = runTest {
        val user = sampleUser()
        val tenantUseCase = FakeGetCurrentTenantUseCase(Result.success(null))
        val userUseCase = FakeGetCurrentUserUseCase(Result.success(user))
        val container = HomeContainer(
            getCurrentTenantUseCase = tenantUseCase,
            getCurrentUserUseCase = userUseCase,
            logoutUseCase = FakeLogoutUseCase()
        )

        container.store.subscribeAndTest {
            emit(HomeIntent.ScreenAppeared)
            advanceUntilIdle()

            val ready = assertIs<HomeState.Ready>(states.value)
            val tenantState = assertIs<DokusState.Success<Tenant?>>(ready.tenantState)
            val userState = assertIs<DokusState.Success<User>>(ready.userState)
            assertNull(tenantState.data)
            assertEquals(user, userState.data)
        }
    }

    @Test
    fun `logout sets loading state and invokes use case`() = runTest {
        val gate = CompletableDeferred<Result<Unit>>()
        val logoutUseCase = FakeLogoutUseCase { gate.await() }
        val container = HomeContainer(
            getCurrentTenantUseCase = FakeGetCurrentTenantUseCase(Result.success(sampleTenant())),
            getCurrentUserUseCase = FakeGetCurrentUserUseCase(Result.success(sampleUser())),
            logoutUseCase = logoutUseCase
        )

        container.store.subscribeAndTest {
            emit(HomeIntent.Logout)
            advanceUntilIdle()

            val inProgress = assertIs<HomeState.Ready>(states.value)
            assertTrue(inProgress.isLoggingOut)
            assertEquals(1, logoutUseCase.invocations)

            gate.complete(Result.success(Unit))
            advanceUntilIdle()

            val completed = assertIs<HomeState.Ready>(states.value)
            assertFalse(completed.isLoggingOut)
        }
    }

    @Test
    fun `logout failure emits show error action`() = runTest {
        val expectedError = DokusException.NotAuthenticated()
        val logoutUseCase = FakeLogoutUseCase { Result.failure(expectedError) }
        val container = HomeContainer(
            getCurrentTenantUseCase = FakeGetCurrentTenantUseCase(Result.success(sampleTenant())),
            getCurrentUserUseCase = FakeGetCurrentUserUseCase(Result.success(sampleUser())),
            logoutUseCase = logoutUseCase
        )

        container.store.subscribeAndTest {
            HomeIntent.Logout resultsIn HomeAction.ShowError(expectedError)
            val ready = assertIs<HomeState.Ready>(states.value)
            assertFalse(ready.isLoggingOut)
            assertEquals(1, logoutUseCase.invocations)
        }
    }
}

private class FakeGetCurrentTenantUseCase(
    private val result: Result<Tenant?>
) : GetCurrentTenantUseCase {
    var invocations: Int = 0

    override suspend fun invoke(): Result<Tenant?> {
        invocations += 1
        return result
    }
}

private class FakeGetCurrentUserUseCase(
    private val result: Result<User>
) : GetCurrentUserUseCase {
    var invocations: Int = 0

    override suspend fun invoke(): Result<User> {
        invocations += 1
        return result
    }
}

private class FakeLogoutUseCase(
    private val block: suspend () -> Result<Unit> = { Result.success(Unit) }
) : LogoutUseCase {
    var invocations: Int = 0

    override suspend fun invoke(): Result<Unit> {
        invocations += 1
        return block()
    }
}

private fun sampleTenant(): Tenant = Tenant(
    id = TenantId("00000000-0000-0000-0000-000000000001"),
    type = TenantType.Company,
    legalName = LegalName("Dokus Ltd"),
    displayName = DisplayName("Dokus"),
    subscription = SubscriptionTier.CoreFounder,
    status = TenantStatus.Active,
    language = Language.En,
    vatNumber = VatNumber("BE0123456789"),
    createdAt = LocalDateTime(2024, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2024, 1, 1, 0, 0),
    avatar = null
)

private fun sampleUser(): User = User(
    id = UserId("00000000-0000-0000-0000-000000000002"),
    email = Email("artem@invoid.vision"),
    firstName = Name("Artem"),
    lastName = Name("Kolesnikov"),
    createdAt = LocalDateTime(2024, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2024, 1, 1, 0, 0),
)
