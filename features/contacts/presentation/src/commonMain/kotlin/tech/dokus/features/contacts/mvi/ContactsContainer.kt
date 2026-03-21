@file:Suppress(
    "TooManyFunctions", // Container handles contacts list workflow
    "TooGenericExceptionCaught" // Network/cache failures need catch-all handling
)

package tech.dokus.features.contacts.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
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
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
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

    override val store: Store<ContactsState, ContactsIntent, ContactsAction> =
        store(ContactsState.initial) {
            init {
                handleRefresh()
            }

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
        updateState { copy(contacts = contacts.asLoading) }
        loadPage(page = 0, reset = true)
    }

    private suspend fun ContactsCtx.handleLoadMore() {
        withState {
            val paginationState =
                contacts.let { if (it.isSuccess()) it.data else return@withState }

            if (!paginationState.hasMorePages) return@withState

            updateState { copy(contacts = contacts.asLoading) }

            val nextPage = paginationState.currentPage + 1
            loadPage(page = nextPage, reset = false)
        }
    }

    private suspend fun ContactsCtx.handleUpdateSortOption(option: ContactSortOption) {
        withState {
            if (!contacts.isSuccess()) return@withState
            val currentPagination = contacts.data
            val sorted = applySorting(currentPagination.data, option)
            updateState {
                copy(
                    sortOption = option,
                    contacts = DokusState.success(currentPagination.copy(data = sorted))
                )
            }
        }
    }

    private suspend fun ContactsCtx.handleUpdateRoleFilter(filter: ContactRoleFilter) {
        updateState { copy(contacts = contacts.asLoading, roleFilter = filter) }
        loadPage(page = 0, reset = true)
    }

    private suspend fun ContactsCtx.handleUpdateActiveFilter(filter: ContactActiveFilter) {
        updateState { copy(contacts = contacts.asLoading, activeFilter = filter) }
        loadPage(page = 0, reset = true)
    }

    private suspend fun ContactsCtx.handleUpdatePeppolFilter(enabled: Boolean?) {
        updateState { copy(contacts = contacts.asLoading, peppolFilter = enabled) }
        loadPage(page = 0, reset = true)
    }

    private suspend fun ContactsCtx.handleClearFilters() {
        updateState {
            copy(
                contacts = contacts.asLoading,
                sortOption = ContactSortOption.Default,
                roleFilter = ContactRoleFilter.All,
                activeFilter = ContactActiveFilter.All,
                peppolFilter = null,
            )
        }
        loadPage(page = 0, reset = true)
    }

    private suspend fun ContactsCtx.handleSelectContact(contactId: ContactId?) {
        updateState { copy(selectedContactId = contactId) }
    }

    private suspend fun ContactsCtx.handleShowCreateContactPane() {
        updateState { copy(showCreateContactPane = true) }
    }

    private suspend fun ContactsCtx.handleHideCreateContactPane() {
        updateState { copy(showCreateContactPane = false) }
    }

    private suspend fun ContactsCtx.loadPage(page: Int, reset: Boolean) {
        withState {
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
                onSuccess = { newContacts ->
                    logger.i { "Loaded ${newContacts.size} contacts (page=$page)" }
                    val allContacts = if (reset) {
                        newContacts
                    } else {
                        (contacts.lastData?.data ?: emptyList()) + newContacts
                    }
                    val sorted = applySorting(allContacts, sortOption)

                    cacheContactsToLocal(newContacts)

                    updateState {
                        copy(
                            contacts = DokusState.success(
                                PaginationState(
                                    data = sorted,
                                    currentPage = page,
                                    pageSize = ContactsState.PAGE_SIZE,
                                    hasMorePages = newContacts.size >= ContactsState.PAGE_SIZE
                                )
                            )
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load contacts from network" }

                    if (contacts.lastData?.data.isNullOrEmpty()) {
                        val cachedContacts = loadFromCache()
                        if (cachedContacts.isNotEmpty()) {
                            val sorted = applySorting(cachedContacts, sortOption)
                            updateState {
                                copy(
                                    contacts = DokusState.success(
                                        PaginationState(
                                            data = sorted,
                                            currentPage = 0,
                                            pageSize = ContactsState.PAGE_SIZE,
                                            hasMorePages = false
                                        )
                                    )
                                )
                            }
                        } else {
                            updateState {
                                copy(
                                    contacts = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(ContactsIntent.Refresh) },
                                        lastData = contacts.lastData
                                    )
                                )
                            }
                        }
                    } else {
                        updateState {
                            copy(
                                contacts = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(ContactsIntent.Refresh) },
                                    lastData = contacts.lastData
                                )
                            )
                        }
                    }
                }
            )
        }
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
}
