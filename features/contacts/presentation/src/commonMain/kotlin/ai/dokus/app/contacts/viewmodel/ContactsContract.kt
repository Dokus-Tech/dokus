package ai.dokus.app.contacts.viewmodel

import ai.dokus.foundation.design.components.dropdown.FilterOption
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.common.PaginationState
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Contacts screen.
 *
 * The Contacts screen displays a list of contacts with:
 * - Pagination for loading more contacts
 * - Search functionality
 * - Sort and filter options (role, active status, Peppol)
 * - Master-detail layout support (selectedContactId)
 * - Create contact form pane visibility (desktop)
 *
 * Flow:
 * 1. Loading → Initial data fetch
 * 2. Content → Contacts loaded, user can search/sort/filter/paginate
 * 3. Error → Failed to load with retry option (can fallback to cached data)
 */

// ============================================================================
// FILTER OPTIONS
// ============================================================================

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

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface ContactsState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data object Loading : ContactsState

    /**
     * Content state - contacts loaded and ready for display.
     *
     * @property contacts Paginated list of contacts
     * @property searchQuery Current search filter
     * @property sortOption Current sort order
     * @property roleFilter Current role filter (All, Customers, Vendors)
     * @property activeFilter Current active status filter
     * @property peppolFilter Current Peppol enabled filter (null = all)
     * @property selectedContactId Selected contact for detail view in master-detail layout
     * @property showCreateContactPane Whether the create contact form pane is visible (desktop)
     */
    data class Content(
        val contacts: PaginationState<ContactDto>,
        val searchQuery: String = "",
        val sortOption: ContactSortOption = ContactSortOption.Default,
        val roleFilter: ContactRoleFilter = ContactRoleFilter.All,
        val activeFilter: ContactActiveFilter = ContactActiveFilter.All,
        val peppolFilter: Boolean? = null,
        val selectedContactId: ContactId? = null,
        val showCreateContactPane: Boolean = false,
    ) : ContactsState

    /**
     * Error state - failed to load initial data.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : ContactsState, DokusState.Error<Nothing>

    companion object {
        const val PAGE_SIZE = 20
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface ContactsIntent : MVIIntent {

    // === Contact Loading ===

    /** Refresh all contacts from the API */
    data object Refresh : ContactsIntent

    /** Load next page of contacts */
    data object LoadMore : ContactsIntent

    // === Search & Filter ===

    /** Update search query and filter contacts */
    data class UpdateSearchQuery(val query: String) : ContactsIntent

    /** Update sort option */
    data class UpdateSortOption(val option: ContactSortOption) : ContactsIntent

    /** Update role filter (All, Customers, Vendors) */
    data class UpdateRoleFilter(val filter: ContactRoleFilter) : ContactsIntent

    /** Update active status filter */
    data class UpdateActiveFilter(val filter: ContactActiveFilter) : ContactsIntent

    /** Update Peppol enabled filter */
    data class UpdatePeppolFilter(val enabled: Boolean?) : ContactsIntent

    /** Clear all filters and reset to defaults */
    data object ClearFilters : ContactsIntent

    // === Selection ===

    /** Select a contact for detail view in master-detail layout */
    data class SelectContact(val contactId: ContactId?) : ContactsIntent

    // === Create Contact Pane (Desktop) ===

    /** Show the create contact form pane */
    data object ShowCreateContactPane : ContactsIntent

    /** Hide the create contact form pane */
    data object HideCreateContactPane : ContactsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface ContactsAction : MVIAction {

    /** Navigate to contact details screen */
    data class NavigateToContactDetails(val contactId: ContactId) : ContactsAction

    /** Navigate to create contact screen (mobile) */
    data object NavigateToCreateContact : ContactsAction

    /** Navigate to edit contact screen */
    data class NavigateToEditContact(val contactId: ContactId) : ContactsAction

    /** Show error message as snackbar/toast */
    data class ShowError(val message: String) : ContactsAction

    /** Show success message as snackbar/toast */
    data class ShowSuccess(val message: String) : ContactsAction
}
