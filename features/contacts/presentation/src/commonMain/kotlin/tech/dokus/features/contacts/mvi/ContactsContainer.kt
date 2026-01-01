package tech.dokus.features.contacts.mvi

import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.ListCustomersUseCase
import tech.dokus.features.contacts.usecases.ListVendorsUseCase
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce

internal typealias ContactsCtx = PipelineContext<ContactsState, ContactsIntent, ContactsAction>

/**
 * Container for the Contacts screen using FlowMVI.
 *
 * Manages contacts with pagination, search, sort, and filter capabilities.
 * Implements cache-first strategy for offline support.
 *
 * Features:
 * - Pagination for contacts with load more
 * - Search with debouncing (cancels previous search on new query)
 * - Sort and filter options (role, active status, Peppol)
 * - Cache-first data loading with offline fallback
 * - Master-detail layout support (selectedContactId)
 * - Create contact pane visibility (desktop)
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ContactsContainer(
    private val listContacts: ListContactsUseCase,
    private val listCustomers: ListCustomersUseCase,
    private val listVendors: ListVendorsUseCase,
    private val getCachedContacts: GetCachedContactsUseCase,
    private val cacheContacts: CacheContactsUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
) : Container<ContactsState, ContactsIntent, ContactsAction> {

    private val logger = Logger.forClass<ContactsContainer>()

    // Internal state for search debouncing
    private var searchJob: Job? = null

    // Internal state for loaded contacts and pagination
    private var loadedContacts: List<ContactDto> = emptyList()
    private var paginationInfo = PaginationInfo()

    override val store: Store<ContactsState, ContactsIntent, ContactsAction> =
        store(ContactsState.Loading) {
            reduce { intent ->
                when (intent) {
                    // Contact Loading
                    is ContactsIntent.Refresh -> handleRefresh()
                    is ContactsIntent.LoadMore -> handleLoadMore()

                    // Search & Filter
                    is ContactsIntent.UpdateSearchQuery -> handleUpdateSearchQuery(intent.query)
                    is ContactsIntent.UpdateSortOption -> handleUpdateSortOption(intent.option)
                    is ContactsIntent.UpdateRoleFilter -> handleUpdateRoleFilter(intent.filter)
                    is ContactsIntent.UpdateActiveFilter -> handleUpdateActiveFilter(intent.filter)
                    is ContactsIntent.UpdatePeppolFilter -> handleUpdatePeppolFilter(intent.enabled)
                    is ContactsIntent.ClearFilters -> handleClearFilters()

                    // Selection
                    is ContactsIntent.SelectContact -> handleSelectContact(intent.contactId)

                    // Create Contact Pane
                    is ContactsIntent.ShowCreateContactPane -> handleShowCreateContactPane()
                    is ContactsIntent.HideCreateContactPane -> handleHideCreateContactPane()
                }
            }
        }

    // === Contact Loading ===

    private suspend fun ContactsCtx.handleRefresh() {
        searchJob?.cancel()
        logger.d { "Refreshing contacts data" }

        // Reset pagination state
        loadedContacts = emptyList()
        paginationInfo = PaginationInfo()

        updateState { ContactsState.Loading }

        loadPage(page = 0, reset = true, filterState = FilterState())
    }

    private suspend fun ContactsCtx.handleLoadMore() {
        withState<ContactsState.Content, _> {
            // Check if we can load more
            if (paginationInfo.isLoadingMore || !paginationInfo.hasMorePages) return@withState

            paginationInfo = paginationInfo.copy(isLoadingMore = true)
            updateState {
                copy(contacts = buildPaginationState())
            }

            val nextPage = paginationInfo.currentPage + 1
            val currentFilterState = FilterState(
                searchQuery = searchQuery,
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                peppolFilter = peppolFilter,
                sortOption = sortOption
            )
            loadPage(page = nextPage, reset = false, filterState = currentFilterState)
        }
    }

    // === Search & Filter ===

    private suspend fun ContactsCtx.handleUpdateSearchQuery(query: String) {
        val trimmed = query.trim()

        withState<ContactsState.Content, _> {
            // Cancel previous search
            searchJob?.cancel()

            // Capture current filter state before updating
            val currentFilterState = FilterState(
                searchQuery = trimmed, // Use the new search query
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                peppolFilter = peppolFilter,
                sortOption = sortOption
            )

            // Reset pagination for new search
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            // Update state immediately with new query
            updateState {
                copy(
                    searchQuery = trimmed,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            // Start new search/load job
            searchJob = launch {
                loadPage(page = 0, reset = true, filterState = currentFilterState)
            }
        }
    }

    private suspend fun ContactsCtx.handleUpdateSortOption(option: ContactSortOption) {
        withState<ContactsState.Content, _> {
            val sorted = applySorting(loadedContacts, option)
            updateState {
                copy(
                    sortOption = option,
                    contacts = contacts.copy(data = sorted)
                )
            }
        }
    }

    private suspend fun ContactsCtx.handleUpdateRoleFilter(filter: ContactRoleFilter) {
        withState<ContactsState.Content, _> {
            // Capture current filter state with updated role
            val currentFilterState = FilterState(
                searchQuery = searchQuery,
                roleFilter = filter, // Use the new filter
                activeFilter = activeFilter,
                peppolFilter = peppolFilter,
                sortOption = sortOption
            )

            // Reset pagination and reload with new filter
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    roleFilter = filter,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(page = 0, reset = true, filterState = currentFilterState)
        }
    }

    private suspend fun ContactsCtx.handleUpdateActiveFilter(filter: ContactActiveFilter) {
        withState<ContactsState.Content, _> {
            // Capture current filter state with updated active filter
            val currentFilterState = FilterState(
                searchQuery = searchQuery,
                roleFilter = roleFilter,
                activeFilter = filter, // Use the new filter
                peppolFilter = peppolFilter,
                sortOption = sortOption
            )

            // Reset pagination and reload with new filter
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    activeFilter = filter,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(page = 0, reset = true, filterState = currentFilterState)
        }
    }

    private suspend fun ContactsCtx.handleUpdatePeppolFilter(enabled: Boolean?) {
        withState<ContactsState.Content, _> {
            // Capture current filter state with updated Peppol filter
            val currentFilterState = FilterState(
                searchQuery = searchQuery,
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                peppolFilter = enabled, // Use the new filter
                sortOption = sortOption
            )

            // Reset pagination and reload with new filter
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    peppolFilter = enabled,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(page = 0, reset = true, filterState = currentFilterState)
        }
    }

    private suspend fun ContactsCtx.handleClearFilters() {
        withState<ContactsState.Content, _> {
            // Reset all filters and pagination
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    searchQuery = "",
                    sortOption = ContactSortOption.Default,
                    roleFilter = ContactRoleFilter.All,
                    activeFilter = ContactActiveFilter.All,
                    peppolFilter = null,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            // Use default filter state (all filters cleared)
            loadPage(page = 0, reset = true, filterState = FilterState())
        }
    }

    // === Selection ===

    private suspend fun ContactsCtx.handleSelectContact(contactId: ContactId?) {
        withState<ContactsState.Content, _> {
            updateState { copy(selectedContactId = contactId) }
        }
    }

    // === Create Contact Pane ===

    private suspend fun ContactsCtx.handleShowCreateContactPane() {
        withState<ContactsState.Content, _> {
            updateState { copy(showCreateContactPane = true) }
        }
    }

    private suspend fun ContactsCtx.handleHideCreateContactPane() {
        withState<ContactsState.Content, _> {
            updateState { copy(showCreateContactPane = false) }
        }
    }

    // === Private Helpers ===

    /**
     * Load a specific page of contacts from the API.
     * Falls back to cached data when offline.
     *
     * @param page Page number to load
     * @param reset Whether to reset loaded contacts (true for first page)
     * @param filterState Current filter state (use defaults when called from Loading state)
     */
    private suspend fun ContactsCtx.loadPage(
        page: Int,
        reset: Boolean,
        filterState: FilterState = FilterState()
    ) {
        val (searchQuery, roleFilter, activeFilter, peppolFilter, sortOption) = filterState

        val activeFilterValue = when (activeFilter) {
            ContactActiveFilter.All -> null
            ContactActiveFilter.Active -> true
            ContactActiveFilter.Inactive -> false
        }

        val searchQueryValue = searchQuery.takeIf { it.isNotEmpty() }

        // Determine which endpoint to use based on role filter
        val result = when (roleFilter) {
            ContactRoleFilter.All -> listContacts(
                search = searchQueryValue,
                isActive = activeFilterValue,
                peppolEnabled = peppolFilter,
                limit = ContactsState.PAGE_SIZE,
                offset = page * ContactsState.PAGE_SIZE
            )
            ContactRoleFilter.Customers -> listCustomers(
                isActive = activeFilterValue ?: true,
                limit = ContactsState.PAGE_SIZE,
                offset = page * ContactsState.PAGE_SIZE
            )
            ContactRoleFilter.Vendors -> listVendors(
                isActive = activeFilterValue ?: true,
                limit = ContactsState.PAGE_SIZE,
                offset = page * ContactsState.PAGE_SIZE
            )
        }

        result.fold(
            onSuccess = { contacts ->
                logger.i { "Loaded ${contacts.size} contacts (page=$page)" }

                // Update loaded contacts
                loadedContacts = if (reset) contacts else loadedContacts + contacts
                paginationInfo = paginationInfo.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = contacts.size >= ContactsState.PAGE_SIZE
                )

                // Cache the contacts for offline access
                cacheContactsToLocal(contacts)

                // Apply client-side filtering and sorting
                val filtered = applyClientSideSearch(loadedContacts, searchQuery, roleFilter)
                val sorted = applySorting(filtered, sortOption)

                updateState {
                    when (this) {
                        is ContactsState.Content -> copy(
                            contacts = buildPaginationState(sorted)
                        )
                        else -> ContactsState.Content(
                            contacts = buildPaginationState(sorted),
                            searchQuery = searchQuery,
                            sortOption = sortOption,
                            roleFilter = roleFilter,
                            activeFilter = activeFilter,
                            peppolFilter = peppolFilter
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contacts from network" }
                paginationInfo = paginationInfo.copy(isLoadingMore = false, hasMorePages = false)

                // Try to load from cache when network fails
                if (loadedContacts.isEmpty()) {
                    val cachedContacts = loadFromCache()
                    if (cachedContacts.isNotEmpty()) {
                        logger.i { "Loaded ${cachedContacts.size} contacts from cache (offline mode)" }
                        loadedContacts = cachedContacts
                        paginationInfo = paginationInfo.copy(
                            currentPage = 0,
                            hasMorePages = false // All cached data loaded at once
                        )

                        val filtered = applyClientSideSearch(loadedContacts, searchQuery, roleFilter)
                        val sorted = applySorting(filtered, sortOption)

                        updateState {
                            ContactsState.Content(
                                contacts = buildPaginationState(sorted),
                                searchQuery = searchQuery,
                                sortOption = sortOption,
                                roleFilter = roleFilter,
                                activeFilter = activeFilter,
                                peppolFilter = peppolFilter
                            )
                        }
                    } else {
                        updateState {
                            ContactsState.Error(
                                exception = error.asDokusException,
                                retryHandler = { intent(ContactsIntent.Refresh) }
                            )
                        }
                    }
                } else {
                    // Keep existing data, just update pagination state
                    withState<ContactsState.Content, _> {
                        updateState {
                            copy(contacts = buildPaginationState())
                        }
                    }
                }
            }
        )
    }

    /**
     * Load contacts from local cache.
     */
    private suspend fun loadFromCache(): List<ContactDto> {
        val tenantId = getTenantId() ?: return emptyList()
        return try {
            getCachedContacts(tenantId)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load contacts from cache" }
            emptyList()
        }
    }

    /**
     * Cache contacts for offline access.
     */
    private suspend fun cacheContactsToLocal(contacts: List<ContactDto>) {
        val tenantId = getTenantId() ?: return
        try {
            cacheContacts(tenantId, contacts)
            logger.d { "Cached ${contacts.size} contacts for offline access" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contacts" }
        }
    }

    /**
     * Get current tenant ID from local JWT claims (no network call).
     */
    private suspend fun getTenantId(): TenantId? {
        return getCurrentTenantId()
    }

    /**
     * Apply client-side search filtering (for Customers/Vendors endpoints that don't support search).
     */
    private fun applyClientSideSearch(
        contacts: List<ContactDto>,
        searchQuery: String,
        roleFilter: ContactRoleFilter
    ): List<ContactDto> {
        val query = searchQuery.lowercase()
        if (query.isEmpty() || roleFilter == ContactRoleFilter.All) {
            return contacts
        }
        // Client-side search for Customers/Vendors endpoints
        return contacts.filter { contact ->
            contact.name.value.lowercase().contains(query) ||
                contact.email?.value?.lowercase()?.contains(query) == true ||
                contact.vatNumber?.value?.lowercase()?.contains(query) == true ||
                contact.phone?.lowercase()?.contains(query) == true
        }
    }

    /**
     * Apply sorting based on the selected sort option.
     */
    private fun applySorting(contacts: List<ContactDto>, sortOption: ContactSortOption): List<ContactDto> {
        return when (sortOption) {
            ContactSortOption.Default -> contacts
            ContactSortOption.NameAsc -> contacts.sortedBy { it.name.value.lowercase() }
            ContactSortOption.NameDesc -> contacts.sortedByDescending { it.name.value.lowercase() }
            ContactSortOption.CreatedNewest -> contacts.sortedByDescending { it.createdAt }
            ContactSortOption.CreatedOldest -> contacts.sortedBy { it.createdAt }
            ContactSortOption.ActivityRecent -> contacts.sortedByDescending {
                it.activitySummary?.lastActivityDate ?: it.updatedAt
            }
        }
    }

    /**
     * Builds the current pagination state for contacts.
     */
    private fun buildPaginationState(data: List<ContactDto> = loadedContacts): PaginationState<ContactDto> {
        return PaginationState(
            data = data,
            currentPage = paginationInfo.currentPage,
            pageSize = ContactsState.PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    /**
     * Helper extension to build pagination state from Content state.
     */
    private fun ContactsState.Content.buildPaginationState(): PaginationState<ContactDto> {
        val filtered = applyClientSideSearch(loadedContacts, searchQuery, roleFilter)
        val sorted = applySorting(filtered, sortOption)
        return PaginationState(
            data = sorted,
            currentPage = paginationInfo.currentPage,
            pageSize = ContactsState.PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    /**
     * Internal pagination tracking.
     */
    private data class PaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )

    /**
     * Helper data class for extracting filter state.
     */
    private data class FilterState(
        val searchQuery: String = "",
        val roleFilter: ContactRoleFilter = ContactRoleFilter.All,
        val activeFilter: ContactActiveFilter = ContactActiveFilter.All,
        val peppolFilter: Boolean? = null,
        val sortOption: ContactSortOption = ContactSortOption.Default
    )
}
