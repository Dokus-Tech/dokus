package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.foundation.design.components.dropdown.FilterOption
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.platform.Logger
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel

/**
 * Sort options for contacts list.
 */
enum class ContactSortOption(override val displayName: String) : FilterOption {
    Default("Default"),
    NameAsc("Name (A-Z)"),
    NameDesc("Name (Z-A)"),
    CreatedNewest("Created (Newest)"),
    CreatedOldest("Created (Oldest)"),
    ActivityRecent("Recent Activity")
}

/**
 * Filter options for contact role.
 */
enum class ContactRoleFilter(override val displayName: String) : FilterOption {
    All("All"),
    Customers("Customers"),
    Vendors("Vendors")
}

/**
 * Filter options for active status.
 */
enum class ContactActiveFilter(override val displayName: String) : FilterOption {
    All("All"),
    Active("Active"),
    Inactive("Inactive")
}

/**
 * ViewModel for the Contacts list screen managing contacts, search, sort, filter, and pagination.
 */
@OptIn(ExperimentalTime::class)
internal class ContactsViewModel :
    BaseViewModel<DokusState<PaginationState<ContactDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<ContactsViewModel>()

    private val contactRepository: ContactRepository by inject()

    private val loadedContacts = MutableStateFlow<List<ContactDto>>(emptyList())
    private val paginationState = MutableStateFlow(PaginationState<ContactDto>(pageSize = PAGE_SIZE))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchJob: Job? = null

    private val _sortOption = MutableStateFlow(ContactSortOption.Default)
    val sortOption: StateFlow<ContactSortOption> = _sortOption.asStateFlow()

    private val _roleFilter = MutableStateFlow(ContactRoleFilter.All)
    val roleFilter: StateFlow<ContactRoleFilter> = _roleFilter.asStateFlow()

    private val _activeFilter = MutableStateFlow(ContactActiveFilter.All)
    val activeFilter: StateFlow<ContactActiveFilter> = _activeFilter.asStateFlow()

    private val _peppolFilter = MutableStateFlow<Boolean?>(null)
    val peppolFilter: StateFlow<Boolean?> = _peppolFilter.asStateFlow()

    private val _selectedContactId = MutableStateFlow<ContactId?>(null)
    val selectedContactId: StateFlow<ContactId?> = _selectedContactId.asStateFlow()

    // Form pane visibility for desktop create/edit
    private val _showCreateContactPane = MutableStateFlow(false)
    val showCreateContactPane: StateFlow<Boolean> = _showCreateContactPane.asStateFlow()

    /**
     * Update the sort option and re-emit the current data sorted.
     */
    fun updateSortOption(option: ContactSortOption) {
        _sortOption.value = option
        emitSuccess()
    }

    /**
     * Update the role filter and reload contacts.
     */
    fun updateRoleFilter(filter: ContactRoleFilter) {
        _roleFilter.value = filter
        refresh()
    }

    /**
     * Update the active status filter and reload contacts.
     */
    fun updateActiveFilter(filter: ContactActiveFilter) {
        _activeFilter.value = filter
        refresh()
    }

    /**
     * Update the Peppol enabled filter and reload contacts.
     */
    fun updatePeppolFilter(enabled: Boolean?) {
        _peppolFilter.value = enabled
        refresh()
    }

    /**
     * Select a contact for detail view in master-detail layout.
     */
    fun selectContact(contactId: ContactId?) {
        _selectedContactId.value = contactId
    }

    /**
     * Show the create contact form pane (desktop only).
     */
    fun showCreateContactPane() {
        _showCreateContactPane.value = true
    }

    /**
     * Hide the create contact form pane.
     */
    fun hideCreateContactPane() {
        _showCreateContactPane.value = false
    }

    /**
     * Refresh the contacts list from the API.
     */
    fun refresh() {
        searchJob?.cancel()
        scope.launch {
            logger.d { "Refreshing contacts data" }
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedContacts.value = emptyList()
            mutableState.emitLoading()

            loadPage(page = 0, reset = true)
        }
    }

    /**
     * Load the next page of contacts for infinite scroll.
     */
    fun loadNextPage() {
        val current = paginationState.value
        if (current.isLoadingMore || !current.hasMorePages) return
        scope.launch {
            paginationState.value = current.copy(isLoadingMore = true)
            emitSuccess()
            loadPage(page = current.currentPage + 1, reset = false)
        }
    }

    /**
     * Update the search query and reload contacts.
     */
    fun updateSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        searchJob?.cancel()
        searchJob = scope.launch {
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedContacts.value = emptyList()
            mutableState.emitLoading()
            loadPage(page = 0, reset = true)
        }
    }

    /**
     * Clear all filters and reset to default state.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _sortOption.value = ContactSortOption.Default
        _roleFilter.value = ContactRoleFilter.All
        _activeFilter.value = ContactActiveFilter.All
        _peppolFilter.value = null
        refresh()
    }

    /**
     * Load a specific page of contacts from the API.
     */
    private suspend fun loadPage(page: Int, reset: Boolean) {
        val activeFilter = when (_activeFilter.value) {
            ContactActiveFilter.All -> null
            ContactActiveFilter.Active -> true
            ContactActiveFilter.Inactive -> false
        }

        val searchQuery = _searchQuery.value.takeIf { it.isNotEmpty() }

        // Determine which endpoint to use based on role filter
        val result = when (_roleFilter.value) {
            ContactRoleFilter.All -> contactRepository.listContacts(
                search = searchQuery,
                isActive = activeFilter,
                peppolEnabled = _peppolFilter.value,
                limit = PAGE_SIZE,
                offset = page * PAGE_SIZE
            )
            ContactRoleFilter.Customers -> contactRepository.listCustomers(
                isActive = activeFilter ?: true,
                limit = PAGE_SIZE,
                offset = page * PAGE_SIZE
            )
            ContactRoleFilter.Vendors -> contactRepository.listVendors(
                isActive = activeFilter ?: true,
                limit = PAGE_SIZE,
                offset = page * PAGE_SIZE
            )
        }

        result.fold(
            onSuccess = { contacts ->
                logger.i { "Loaded ${contacts.size} contacts (page=$page)" }
                loadedContacts.value = if (reset) contacts else loadedContacts.value + contacts
                paginationState.value = paginationState.value.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = contacts.size >= PAGE_SIZE,
                    pageSize = PAGE_SIZE
                )
                emitSuccess()
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contacts" }
                paginationState.value = paginationState.value.copy(isLoadingMore = false, hasMorePages = false)
                if (loadedContacts.value.isEmpty()) {
                    mutableState.emit(error) { refresh() }
                } else {
                    emitSuccess()
                }
            }
        )
    }

    /**
     * Apply sorting and filtering to loaded contacts and emit success state.
     */
    private fun emitSuccess() {
        val filtered = applyClientSideSearch(loadedContacts.value)
        val sorted = applySorting(filtered)
        val updated = paginationState.value.copy(data = sorted, pageSize = PAGE_SIZE)
        paginationState.value = updated
        mutableState.value = DokusState.success(updated)
    }

    /**
     * Apply client-side search filtering (for Customers/Vendors endpoints that don't support search).
     */
    private fun applyClientSideSearch(contacts: List<ContactDto>): List<ContactDto> {
        val query = _searchQuery.value.lowercase()
        if (query.isEmpty() || _roleFilter.value == ContactRoleFilter.All) {
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
    private fun applySorting(contacts: List<ContactDto>): List<ContactDto> {
        return when (_sortOption.value) {
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

    companion object {
        private const val PAGE_SIZE = 20
    }
}
