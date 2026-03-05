package tech.dokus.features.auth.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceSelectContainerTest {

    @Test
    fun `unauthenticated tenants load emits navigate to login instead of error state`() = runTest {
        val container = WorkspaceSelectContainer(
            listMyTenants = FakeListMyTenantsUseCase(
                Result.failure(DokusException.NotAuthenticated("Authentication required"))
            ),
            selectTenantUseCase = FakeSelectTenantUseCase()
        )

        container.store.subscribeAndTest {
            WorkspaceSelectIntent.LoadTenants resultsIn WorkspaceSelectAction.NavigateToLogin
            assertIs<WorkspaceSelectState.Loading>(states.value)
        }
    }
}

private class FakeListMyTenantsUseCase(
    private val result: Result<List<Tenant>>
) : ListMyTenantsUseCase {
    override suspend fun invoke(): Result<List<Tenant>> = result
}

private class FakeSelectTenantUseCase : SelectTenantUseCase {
    override suspend fun invoke(tenantId: TenantId): Result<Unit> = Result.success(Unit)
}
