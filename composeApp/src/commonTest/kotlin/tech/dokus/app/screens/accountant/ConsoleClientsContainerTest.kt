package tech.dokus.app.screens.accountant

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DisplayName
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.features.auth.usecases.ListConsoleClientsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsoleClientsContainerTest {

    @Test
    fun `load sorts clients and query filters by name and vat`() = runTest {
        val clients = listOf(
            client(
                tenantId = "00000000-0000-0000-0000-000000000300",
                companyName = "Zeta BV",
                vat = null
            ),
            client(
                tenantId = "00000000-0000-0000-0000-000000000200",
                companyName = "Acme BV",
                vat = "BE02.000.000.000"
            ),
            client(
                tenantId = "00000000-0000-0000-0000-000000000100",
                companyName = "Acme BV",
                vat = "BE01.000.000.000"
            ),
        )
        val listUseCase = AccountantFakeListConsoleClientsUseCase(Result.success(clients))
        val container = ConsoleClientsContainer(
            listConsoleClientsUseCase = listUseCase,
            selectTenantUseCase = AccountantFakeSelectTenantUseCase { Result.success(Unit) }
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(1, listUseCase.invocations)

            val loaded = assertIs<ConsoleClientsState.Content>(states.value)
            assertEquals(
                listOf(
                    TenantId("00000000-0000-0000-0000-000000000100"),
                    TenantId("00000000-0000-0000-0000-000000000200"),
                    TenantId("00000000-0000-0000-0000-000000000300"),
                ),
                loaded.clients.map { it.tenantId }
            )

            emit(ConsoleClientsIntent.UpdateQuery("be01"))
            advanceUntilIdle()

            val filtered = assertIs<ConsoleClientsState.Content>(states.value)
            assertEquals(
                listOf(TenantId("00000000-0000-0000-0000-000000000100")),
                filtered.filteredClients.map { it.tenantId }
            )
        }
    }

    @Test
    fun `load failure maps to error state`() = runTest {
        val expectedError = DokusException.NotAuthenticated()
        val container = ConsoleClientsContainer(
            listConsoleClientsUseCase = AccountantFakeListConsoleClientsUseCase(Result.failure(expectedError)),
            selectTenantUseCase = AccountantFakeSelectTenantUseCase { Result.success(Unit) }
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            val error = assertIs<ConsoleClientsState.Error>(states.value)
            assertEquals(expectedError, error.exception)
        }
    }

    @Test
    fun `select client success navigates to documents`() = runTest {
        val tenantId = TenantId("00000000-0000-0000-0000-000000000555")
        val selectUseCase = AccountantFakeSelectTenantUseCase { Result.success(Unit) }
        val container = ConsoleClientsContainer(
            listConsoleClientsUseCase = AccountantFakeListConsoleClientsUseCase(
                Result.success(
                    listOf(
                        client(
                            tenantId = tenantId.toString(),
                            companyName = "Client One",
                            vat = null
                        )
                    )
                )
            ),
            selectTenantUseCase = selectUseCase
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            ConsoleClientsIntent.SelectClient(tenantId) resultsIn ConsoleClientsAction.NavigateToDocuments

            assertEquals(listOf(tenantId), selectUseCase.selectedTenantIds)
            val loaded = assertIs<ConsoleClientsState.Content>(states.value)
            assertEquals(tenantId, loaded.selectingTenantId)
        }
    }

    @Test
    fun `select client failure keeps user on list and shows error`() = runTest {
        val tenantId = TenantId("00000000-0000-0000-0000-000000000666")
        val expectedError = DokusException.NotAuthenticated()
        val selectUseCase = AccountantFakeSelectTenantUseCase { Result.failure(expectedError) }
        val container = ConsoleClientsContainer(
            listConsoleClientsUseCase = AccountantFakeListConsoleClientsUseCase(
                Result.success(
                    listOf(
                        client(
                            tenantId = tenantId.toString(),
                            companyName = "Client One",
                            vat = null
                        )
                    )
                )
            ),
            selectTenantUseCase = selectUseCase
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            ConsoleClientsIntent.SelectClient(tenantId) resultsIn ConsoleClientsAction.ShowError(expectedError)

            val loaded = assertIs<ConsoleClientsState.Content>(states.value)
            assertEquals(null, loaded.selectingTenantId)
            assertEquals(listOf(tenantId), selectUseCase.selectedTenantIds)
            assertTrue(loaded.filteredClients.isNotEmpty())
        }
    }
}

private class AccountantFakeListConsoleClientsUseCase(
    private val result: Result<List<ConsoleClientSummary>>
) : ListConsoleClientsUseCase {
    var invocations: Int = 0

    override suspend fun invoke(): Result<List<ConsoleClientSummary>> {
        invocations += 1
        return result
    }
}

private class AccountantFakeSelectTenantUseCase(
    private val resultProvider: suspend (TenantId) -> Result<Unit>
) : SelectTenantUseCase {
    val selectedTenantIds = mutableListOf<TenantId>()

    override suspend fun invoke(tenantId: TenantId): Result<Unit> {
        selectedTenantIds += tenantId
        return resultProvider(tenantId)
    }
}

private fun client(
    tenantId: String,
    companyName: String,
    vat: String?
): ConsoleClientSummary {
    return ConsoleClientSummary(
        tenantId = TenantId(tenantId),
        companyName = DisplayName(companyName),
        vatNumber = vat?.let(::VatNumber)
    )
}
