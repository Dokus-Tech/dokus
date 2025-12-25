package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantIdUseCase
import ai.dokus.app.contacts.repository.ContactCacheApi
import ai.dokus.app.contacts.repository.ContactRepositoryApi
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.enums.ClientType
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactMergeResult
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.domain.model.ContactStats
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.domain.model.common.PaginationState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.foundation.app.state.CacheState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for ContactsContainer FlowMVI implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactsContainerTest {

    // ============================================================================
    // TEST FAKES
    // ============================================================================

    /**
     * Fake implementation of ContactRepositoryApi for testing.
     */
    private class FakeContactRepository : ContactRepositoryApi {
        var listContactsResult: Result<List<ContactDto>> = Result.success(emptyList())
        var listCustomersResult: Result<List<ContactDto>> = Result.success(emptyList())
        var listVendorsResult: Result<List<ContactDto>> = Result.success(emptyList())
        var listContactsCallCount = 0
        var lastSearchParam: String? = null
        var lastOffsetParam: Int = 0
        var lastLimitParam: Int = 0

        override suspend fun listContacts(
            search: String?,
            isActive: Boolean?,
            peppolEnabled: Boolean?,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> {
            listContactsCallCount++
            lastSearchParam = search
            lastOffsetParam = offset
            lastLimitParam = limit
            return listContactsResult
        }

        override suspend fun listCustomers(
            isActive: Boolean,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = listCustomersResult

        override suspend fun listVendors(
            isActive: Boolean,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = listVendorsResult

        override suspend fun getContact(contactId: ContactId): Result<ContactDto> =
            Result.failure(NotImplementedError())

        override suspend fun createContact(request: CreateContactRequest): Result<ContactDto> =
            Result.failure(NotImplementedError())

        override suspend fun updateContact(
            contactId: ContactId,
            request: UpdateContactRequest
        ): Result<ContactDto> = Result.failure(NotImplementedError())

        override suspend fun deleteContact(contactId: ContactId): Result<Unit> =
            Result.failure(NotImplementedError())

        override suspend fun updateContactPeppol(
            contactId: ContactId,
            request: UpdateContactPeppolRequest
        ): Result<ContactDto> = Result.failure(NotImplementedError())

        override suspend fun getContactActivity(contactId: ContactId): Result<ContactActivitySummary> =
            Result.failure(NotImplementedError())

        override suspend fun getContactStats(): Result<ContactStats> =
            Result.failure(NotImplementedError())

        override suspend fun mergeContacts(
            sourceContactId: ContactId,
            targetContactId: ContactId
        ): Result<ContactMergeResult> = Result.failure(NotImplementedError())

        override suspend fun listNotes(
            contactId: ContactId,
            limit: Int,
            offset: Int
        ): Result<List<ContactNoteDto>> = Result.failure(NotImplementedError())

        override suspend fun createNote(
            contactId: ContactId,
            request: CreateContactNoteRequest
        ): Result<ContactNoteDto> = Result.failure(NotImplementedError())

        override suspend fun updateNote(
            contactId: ContactId,
            noteId: ContactNoteId,
            request: UpdateContactNoteRequest
        ): Result<ContactNoteDto> = Result.failure(NotImplementedError())

        override suspend fun deleteNote(
            contactId: ContactId,
            noteId: ContactNoteId
        ): Result<Unit> = Result.failure(NotImplementedError())
    }

    /**
     * Fake implementation of ContactCacheApi for testing.
     */
    private class FakeContactCache : ContactCacheApi {
        var cachedContacts: List<ContactDto> = emptyList()
        var cacheContactsCalled = false
        var getCachedContactsCalled = false

        override fun observeContacts(
            tenantId: TenantId,
            search: String?,
            isActive: Boolean?,
            forceRefresh: Boolean
        ): Flow<CacheState<List<ContactDto>>> = emptyFlow()

        override fun observeContact(
            contactId: ContactId,
            tenantId: TenantId,
            forceRefresh: Boolean
        ): Flow<CacheState<ContactDto>> = emptyFlow()

        override suspend fun getCachedContacts(tenantId: TenantId): List<ContactDto> {
            getCachedContactsCalled = true
            return cachedContacts
        }

        override suspend fun getLastSyncTime(tenantId: TenantId): Long? = null

        override suspend fun clearCache(tenantId: TenantId) {}

        override suspend fun cacheContacts(tenantId: TenantId, contacts: List<ContactDto>) {
            cacheContactsCalled = true
            cachedContacts = contacts
        }
    }

    /**
     * Fake implementation of GetCurrentTenantIdUseCase.
     */
    private class FakeGetCurrentTenantId(
        private val tenantId: TenantId? = TenantId.generate()
    ) : GetCurrentTenantIdUseCase {
        override suspend fun invoke(): TenantId? = tenantId
    }

    // ============================================================================
    // TEST DATA FACTORY
    // ============================================================================

    private val testTenantId = TenantId.generate()
    private val now = LocalDateTime(2024, 1, 1, 12, 0, 0)

    private fun createContact(
        id: ContactId = ContactId.generate(),
        name: String = "Test Contact",
        email: String? = "test@example.com"
    ) = ContactDto(
        id = id,
        tenantId = testTenantId,
        name = Name(name),
        email = email?.let { Email(it) },
        phone = null,
        vatNumber = null,
        companyNumber = null,
        businessType = ClientType.Business,
        addressLine1 = null,
        addressLine2 = null,
        city = null,
        postalCode = null,
        country = null,
        peppolId = null,
        peppolEnabled = false,
        defaultPaymentTerms = 30,
        defaultVatRate = null,
        tags = null,
        isActive = true,
        activitySummary = null,
        createdAt = now,
        updatedAt = now
    )

    private fun createContainer(
        contactRepository: ContactRepositoryApi = FakeContactRepository(),
        contactCache: ContactCacheApi = FakeContactCache(),
        getCurrentTenantId: GetCurrentTenantIdUseCase = FakeGetCurrentTenantId(testTenantId)
    ): ContactsContainer {
        return ContactsContainer(contactRepository, contactCache, getCurrentTenantId)
    }

    // ============================================================================
    // CONTRACT TESTS (Verify State Machine Behavior)
    // ============================================================================

    @Test
    fun `initial state is Loading`() = runTest {
        val container = createContainer()

        container.store.subscribeAndTest {
            assertIs<ContactsState.Loading>(states.value)
        }
    }

    @Test
    fun `Content state holds contacts and filter options`() = runTest {
        val contacts = listOf(createContact(name = "Alice"), createContact(name = "Bob"))
        val paginationState = PaginationState(
            data = contacts,
            currentPage = 0,
            pageSize = 20,
            hasMorePages = false,
            isLoadingMore = false
        )

        val contentState = ContactsState.Content(
            contacts = paginationState,
            searchQuery = "test",
            sortOption = ContactSortOption.NameAsc,
            roleFilter = ContactRoleFilter.Customers,
            activeFilter = ContactActiveFilter.Active,
            peppolFilter = true,
            selectedContactId = contacts[0].id,
            showCreateContactPane = true
        )

        assertEquals(2, contentState.contacts.data.size)
        assertEquals("test", contentState.searchQuery)
        assertEquals(ContactSortOption.NameAsc, contentState.sortOption)
        assertEquals(ContactRoleFilter.Customers, contentState.roleFilter)
        assertEquals(ContactActiveFilter.Active, contentState.activeFilter)
        assertEquals(true, contentState.peppolFilter)
        assertEquals(contacts[0].id, contentState.selectedContactId)
        assertTrue(contentState.showCreateContactPane)
    }

    @Test
    fun `Error state has exception and retry handler`() = runTest {
        var retryCalled = false
        val errorState = ContactsState.Error(
            exception = DokusException.Unknown(
                throwable = RuntimeException("Network error")
            ),
            retryHandler = { retryCalled = true }
        )

        assertIs<ContactsState.Error>(errorState)
        assertEquals("Network error", errorState.exception.message)

        errorState.retryHandler.retry()
        assertTrue(retryCalled)
    }

    @Test
    fun `all Intent types are defined`() {
        val intents: List<ContactsIntent> = listOf(
            ContactsIntent.Refresh,
            ContactsIntent.LoadMore,
            ContactsIntent.UpdateSearchQuery("test"),
            ContactsIntent.UpdateSortOption(ContactSortOption.NameAsc),
            ContactsIntent.UpdateRoleFilter(ContactRoleFilter.All),
            ContactsIntent.UpdateActiveFilter(ContactActiveFilter.All),
            ContactsIntent.UpdatePeppolFilter(true),
            ContactsIntent.ClearFilters,
            ContactsIntent.SelectContact(ContactId.generate()),
            ContactsIntent.ShowCreateContactPane,
            ContactsIntent.HideCreateContactPane
        )

        assertEquals(11, intents.size)
    }

    @Test
    fun `all Action types are defined`() {
        val actions: List<ContactsAction> = listOf(
            ContactsAction.NavigateToContactDetails(ContactId.generate()),
            ContactsAction.NavigateToCreateContact,
            ContactsAction.NavigateToEditContact(ContactId.generate()),
            ContactsAction.ShowError("error"),
            ContactsAction.ShowSuccess("success")
        )

        assertEquals(5, actions.size)
    }

    // ============================================================================
    // INTEGRATION TESTS (Verify Container Logic)
    // ============================================================================

    @Test
    fun `refresh loads contacts and transitions to Content`() = runTest {
        val contacts = listOf(
            createContact(name = "Alice"),
            createContact(name = "Bob")
        )
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(contacts)
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            assertIs<ContactsState.Loading>(states.value)
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100) // Give time for state to update

            val currentState = states.value
            assertIs<ContactsState.Content>(currentState)
            assertEquals(2, currentState.contacts.data.size)
        }
    }

    @Test
    fun `refresh with network error and empty cache shows Error state`() = runTest {
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.failure(RuntimeException("Network error"))
        }
        val fakeCache = FakeContactCache().apply {
            cachedContacts = emptyList()
        }

        val container = createContainer(
            contactRepository = fakeRepo,
            contactCache = fakeCache
        )

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            val currentState = states.value
            assertIs<ContactsState.Error>(currentState)
        }
    }

    @Test
    fun `refresh with network error falls back to cache`() = runTest {
        val cachedContacts = listOf(
            createContact(name = "Cached Contact")
        )
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.failure(RuntimeException("Network error"))
        }
        val fakeCache = FakeContactCache().apply {
            this.cachedContacts = cachedContacts
        }

        val container = createContainer(
            contactRepository = fakeRepo,
            contactCache = fakeCache
        )

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            val currentState = states.value
            assertIs<ContactsState.Content>(currentState)
            assertEquals(1, currentState.contacts.data.size)
            assertEquals("Cached Contact", currentState.contacts.data[0].name.value)
        }
    }

    @Test
    fun `successful load caches contacts`() = runTest {
        val contacts = listOf(createContact(name = "Fresh Contact"))
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(contacts)
        }
        val fakeCache = FakeContactCache()

        val container = createContainer(
            contactRepository = fakeRepo,
            contactCache = fakeCache
        )

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            assertTrue(fakeCache.cacheContactsCalled)
            assertEquals(1, fakeCache.cachedContacts.size)
        }
    }

    // ============================================================================
    // FILTER TESTS
    // ============================================================================

    @Test
    fun `role filter Customers calls listCustomers endpoint`() = runTest {
        val customers = listOf(createContact(name = "Customer"))
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(emptyList())
            listCustomersResult = Result.success(customers)
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)
            assertIs<ContactsState.Content>(states.value)

            emit(ContactsIntent.UpdateRoleFilter(ContactRoleFilter.Customers))
            advanceUntilIdle()
            delay(100)

            val currentState = states.value
            assertIs<ContactsState.Content>(currentState)
            assertEquals(ContactRoleFilter.Customers, currentState.roleFilter)
        }
    }

    @Test
    fun `role filter Vendors calls listVendors endpoint`() = runTest {
        val vendors = listOf(createContact(name = "Vendor"))
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(emptyList())
            listVendorsResult = Result.success(vendors)
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)
            assertIs<ContactsState.Content>(states.value)

            emit(ContactsIntent.UpdateRoleFilter(ContactRoleFilter.Vendors))
            advanceUntilIdle()
            delay(100)

            val currentState = states.value
            assertIs<ContactsState.Content>(currentState)
            assertEquals(ContactRoleFilter.Vendors, currentState.roleFilter)
        }
    }

    // ============================================================================
    // SORTING TESTS
    // ============================================================================

    @Test
    fun `ContactSortOption values are correct`() {
        val options = ContactSortOption.entries

        assertEquals(6, options.size)
        assertTrue(options.contains(ContactSortOption.Default))
        assertTrue(options.contains(ContactSortOption.NameAsc))
        assertTrue(options.contains(ContactSortOption.NameDesc))
        assertTrue(options.contains(ContactSortOption.CreatedNewest))
        assertTrue(options.contains(ContactSortOption.CreatedOldest))
        assertTrue(options.contains(ContactSortOption.ActivityRecent))
    }

    @Test
    fun `ContactRoleFilter values are correct`() {
        val filters = ContactRoleFilter.entries

        assertEquals(3, filters.size)
        assertTrue(filters.contains(ContactRoleFilter.All))
        assertTrue(filters.contains(ContactRoleFilter.Customers))
        assertTrue(filters.contains(ContactRoleFilter.Vendors))
    }

    @Test
    fun `ContactActiveFilter values are correct`() {
        val filters = ContactActiveFilter.entries

        assertEquals(3, filters.size)
        assertTrue(filters.contains(ContactActiveFilter.All))
        assertTrue(filters.contains(ContactActiveFilter.Active))
        assertTrue(filters.contains(ContactActiveFilter.Inactive))
    }

    // ============================================================================
    // SELECTION TESTS
    // ============================================================================

    @Test
    fun `selectContact updates selectedContactId in Content state`() = runTest {
        val contacts = listOf(createContact(name = "Alice"))
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(contacts)
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            emit(ContactsIntent.SelectContact(contacts[0].id))
            advanceUntilIdle()
            delay(100)

            val updatedState = states.value
            assertIs<ContactsState.Content>(updatedState)
            assertEquals(contacts[0].id, updatedState.selectedContactId)
        }
    }

    // ============================================================================
    // CREATE PANE TESTS
    // ============================================================================

    @Test
    fun `showCreateContactPane updates state`() = runTest {
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(emptyList())
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            emit(ContactsIntent.ShowCreateContactPane)
            advanceUntilIdle()
            delay(100)

            val state = states.value
            assertIs<ContactsState.Content>(state)
            assertTrue(state.showCreateContactPane)
        }
    }

    @Test
    fun `hideCreateContactPane updates state`() = runTest {
        val fakeRepo = FakeContactRepository().apply {
            listContactsResult = Result.success(emptyList())
        }

        val container = createContainer(contactRepository = fakeRepo)

        container.store.subscribeAndTest {
            emit(ContactsIntent.Refresh)
            advanceUntilIdle()
            delay(100)

            emit(ContactsIntent.ShowCreateContactPane)
            advanceUntilIdle()
            delay(100)

            emit(ContactsIntent.HideCreateContactPane)
            advanceUntilIdle()
            delay(100)

            val state = states.value
            assertIs<ContactsState.Content>(state)
            assertTrue(!state.showCreateContactPane)
        }
    }
}
