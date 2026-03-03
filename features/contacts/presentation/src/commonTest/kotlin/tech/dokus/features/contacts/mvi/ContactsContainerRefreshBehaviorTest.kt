package tech.dokus.features.contacts.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.ListCustomersUseCase
import tech.dokus.features.contacts.usecases.ListVendorsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsContainerRefreshBehaviorTest {

    @Test
    fun `container init performs initial refresh without route-triggered intent`() = runTest {
        val listContacts = FakeListContactsUseCase()
        val container = ContactsContainer(
            listContacts = listContacts,
            listCustomers = FakeListCustomersUseCase(),
            listVendors = FakeListVendorsUseCase(),
            getCachedContacts = FakeGetCachedContactsUseCase(),
            cacheContacts = FakeCacheContactsUseCase(),
            getCurrentTenantId = FakeGetCurrentTenantIdUseCase(),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val state = assertIs<ContactsState.Content>(states.value)
            assertTrue(state.contacts.data.isEmpty())
            assertEquals(1, listContacts.calls.size)
            assertEquals(null, listContacts.calls.single().isActive)
            assertEquals(ContactsState.PAGE_SIZE, listContacts.calls.single().limit)
            assertEquals(0, listContacts.calls.single().offset)
        }
    }
}

private class FakeListContactsUseCase : ListContactsUseCase {
    data class Call(val isActive: Boolean?, val limit: Int, val offset: Int)

    val calls = mutableListOf<Call>()

    override suspend fun invoke(
        isActive: Boolean?,
        limit: Int,
        offset: Int,
    ): Result<List<ContactDto>> {
        calls += Call(isActive = isActive, limit = limit, offset = offset)
        return Result.success(emptyList())
    }
}

private class FakeListCustomersUseCase : ListCustomersUseCase {
    override suspend fun invoke(
        isActive: Boolean,
        limit: Int,
        offset: Int,
    ): Result<List<ContactDto>> = Result.success(emptyList())
}

private class FakeListVendorsUseCase : ListVendorsUseCase {
    override suspend fun invoke(
        isActive: Boolean,
        limit: Int,
        offset: Int,
    ): Result<List<ContactDto>> = Result.success(emptyList())
}

private class FakeGetCachedContactsUseCase : GetCachedContactsUseCase {
    override suspend fun invoke(tenantId: TenantId): List<ContactDto> = emptyList()
}

private class FakeCacheContactsUseCase : CacheContactsUseCase {
    override suspend fun invoke(tenantId: TenantId, contacts: List<ContactDto>) = Unit
}

private class FakeGetCurrentTenantIdUseCase : GetCurrentTenantIdUseCase {
    override suspend fun invoke(): TenantId? = null
}
