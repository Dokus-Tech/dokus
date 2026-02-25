@file:Suppress(
    "TooManyFunctions", // Container handles contacts list workflow
    "TooGenericExceptionCaught" // Network/cache failures need catch-all handling
)

package tech.dokus.features.contacts.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.GetCachedContactsUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.ListCustomersUseCase
import tech.dokus.features.contacts.usecases.ListVendorsUseCase
import tech.dokus.foundation.platform.Logger

internal typealias ContactsCtx = PipelineContext<ContactsState, ContactsIntent, ContactsAction>

internal class ContactsContainer(
    private val listContacts: ListContactsUseCase,
    private val listCustomers: ListCustomersUseCase,
    private val listVendors: ListVendorsUseCase,
    private val getCachedContacts: GetCachedContactsUseCase,
    private val cacheContacts: CacheContactsUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
) : Container<ContactsState, ContactsIntent, ContactsAction> {

    private val logger = Logger.forClass<ContactsContainer>()

    private var loadedContacts: List<ContactDto> = emptyList()
    private var paginationInfo = PaginationInfo()

    override val store: Store<ContactsState, ContactsIntent, ContactsAction> =
        store(ContactsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is ContactsIntent.Refresh -> handleRefresh()
                    is ContactsIntent.LoadMore -> handleLoadMore()
                    is ContactsIntent.UpdateSortOption -> handleUpdateSortOption(intent.option)
                    is ContactsIntent.UpdateRoleFilter -> handleUpdateRoleFilter(intent.filter)
                    is ContactsIntent.UpdateActiveFilter -> handleUpdateActiveFilter(intent.filter)
                    is ContactsIntent.UpdatePeppolFilter -> handleUpdatePeppolFilter(intent.enabled)
                    is ContactsIntent.ClearFilters -> handleClearFilters()
                    is ContactsIntent.SelectContact -> handleSelectContact(intent.contactId)
                    is ContactsIntent.ShowCreateContactPane -> handleShowCreateContactPane()
                    is ContactsIntent.HideCreateContactPane -> handleHideCreateContactPane()
                }
            }
        }

    private suspend fun ContactsCtx.handleRefresh() {
        logger.d { "Refreshing contacts data" }
        loadedContacts = emptyList()
        paginationInfo = PaginationInfo()
        updateState { ContactsState.Loading }
        loadPage(page = 0, reset = true, filterState = FilterState())
    }

    private suspend fun ContactsCtx.handleLoadMore() {
        withState<ContactsState.Content, _> {
            if (paginationInfo.isLoadingMore || !paginationInfo.hasMorePages) return@withState

            paginationInfo = paginationInfo.copy(isLoadingMore = true)
            updateState { copy(contacts = buildPaginationState()) }

            val nextPage = paginationInfo.currentPage + 1
            loadPage(
                page = nextPage,
                reset = false,
                filterState = FilterState(
                    roleFilter = roleFilter,
                    activeFilter = activeFilter,
                    peppolFilter = peppolFilter,
                    sortOption = sortOption
                )
            )
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
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    roleFilter = filter,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(
                page = 0,
                reset = true,
                filterState = FilterState(
                    roleFilter = filter,
                    activeFilter = activeFilter,
                    peppolFilter = peppolFilter,
                    sortOption = sortOption
                )
            )
        }
    }

    private suspend fun ContactsCtx.handleUpdateActiveFilter(filter: ContactActiveFilter) {
        withState<ContactsState.Content, _> {
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    activeFilter = filter,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(
                page = 0,
                reset = true,
                filterState = FilterState(
                    roleFilter = roleFilter,
                    activeFilter = filter,
                    peppolFilter = peppolFilter,
                    sortOption = sortOption
                )
            )
        }
    }

    private suspend fun ContactsCtx.handleUpdatePeppolFilter(enabled: Boolean?) {
        withState<ContactsState.Content, _> {
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    peppolFilter = enabled,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(
                page = 0,
                reset = true,
                filterState = FilterState(
                    roleFilter = roleFilter,
                    activeFilter = activeFilter,
                    peppolFilter = enabled,
                    sortOption = sortOption
                )
            )
        }
    }

    private suspend fun ContactsCtx.handleClearFilters() {
        withState<ContactsState.Content, _> {
            loadedContacts = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    sortOption = ContactSortOption.Default,
                    roleFilter = ContactRoleFilter.All,
                    activeFilter = ContactActiveFilter.All,
                    peppolFilter = null,
                    contacts = PaginationState(pageSize = ContactsState.PAGE_SIZE)
                )
            }

            loadPage(page = 0, reset = true, filterState = FilterState())
        }
    }

    private suspend fun ContactsCtx.handleSelectContact(contactId: ContactId?) {
        withState<ContactsState.Content, _> {
            updateState { copy(selectedContactId = contactId) }
        }
    }

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

    private suspend fun ContactsCtx.loadPage(
        page: Int,
        reset: Boolean,
        filterState: FilterState = FilterState()
    ) {
        val (roleFilter, activeFilter, peppolFilter, sortOption) = filterState

        val activeFilterValue = when (activeFilter) {
            ContactActiveFilter.All -> null
            ContactActiveFilter.Active -> true
            ContactActiveFilter.Inactive -> false
        }

        val result = when (roleFilter) {
            ContactRoleFilter.All -> listContacts(
                isActive = activeFilterValue,
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
                loadedContacts = if (reset) contacts else loadedContacts + contacts
                paginationInfo = paginationInfo.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = contacts.size >= ContactsState.PAGE_SIZE
                )

                cacheContactsToLocal(contacts)

                val sorted = applySorting(loadedContacts, sortOption)
                updateState {
                    when (this) {
                        is ContactsState.Content -> copy(contacts = buildPaginationState(sorted))
                        else -> ContactsState.Content(
                            contacts = buildPaginationState(sorted),
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

                if (loadedContacts.isEmpty()) {
                    val cachedContacts = loadFromCache()
                    if (cachedContacts.isNotEmpty()) {
                        loadedContacts = cachedContacts
                        paginationInfo = paginationInfo.copy(currentPage = 0, hasMorePages = false)
                        val sorted = applySorting(loadedContacts, sortOption)
                        updateState {
                            ContactsState.Content(
                                contacts = buildPaginationState(sorted),
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
                    withState<ContactsState.Content, _> {
                        updateState { copy(contacts = buildPaginationState()) }
                    }
                }
            }
        )
    }

    private suspend fun loadFromCache(): List<ContactDto> {
        val tenantId = getTenantId() ?: return emptyList()
        return try {
            getCachedContacts(tenantId)
        } catch (e: Exception) {
            logger.e(e) { "Failed to load contacts from cache" }
            emptyList()
        }
    }

    private suspend fun cacheContactsToLocal(contacts: List<ContactDto>) {
        val tenantId = getTenantId() ?: return
        try {
            cacheContacts(tenantId, contacts)
            logger.d { "Cached ${contacts.size} contacts for offline access" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contacts" }
        }
    }

    private suspend fun getTenantId(): TenantId? = getCurrentTenantId()

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

    private fun buildPaginationState(data: List<ContactDto> = loadedContacts): PaginationState<ContactDto> {
        return PaginationState(
            data = data,
            currentPage = paginationInfo.currentPage,
            pageSize = ContactsState.PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    private data class PaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )

    private data class FilterState(
        val roleFilter: ContactRoleFilter = ContactRoleFilter.All,
        val activeFilter: ContactActiveFilter = ContactActiveFilter.All,
        val peppolFilter: Boolean? = null,
        val sortOption: ContactSortOption = ContactSortOption.Default
    )
}
