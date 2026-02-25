package tech.dokus.features.contacts.mvi

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_filter_all
import tech.dokus.aura.resources.contacts_filter_customers
import tech.dokus.aura.resources.contacts_filter_vendors
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_sort_activity_recent
import tech.dokus.aura.resources.contacts_sort_created_newest
import tech.dokus.aura.resources.contacts_sort_created_oldest
import tech.dokus.aura.resources.contacts_sort_default
import tech.dokus.aura.resources.contacts_sort_name_asc
import tech.dokus.aura.resources.contacts_sort_name_desc
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.dropdown.FilterOption

/**
 * Contract for the Contacts screen.
 *
 * The Contacts screen displays a list of contacts with:
 * - Pagination for loading more contacts
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
enum class ContactSortOption(override val labelRes: StringResource) : FilterOption {
    Default(Res.string.contacts_sort_default),
    NameAsc(Res.string.contacts_sort_name_asc),
    NameDesc(Res.string.contacts_sort_name_desc),
    CreatedNewest(Res.string.contacts_sort_created_newest),
    CreatedOldest(Res.string.contacts_sort_created_oldest),
    ActivityRecent(Res.string.contacts_sort_activity_recent)
}

/**
 * Filter options for contact role.
 */
enum class ContactRoleFilter(override val labelRes: StringResource) : FilterOption {
    All(Res.string.contacts_filter_all),
    Customers(Res.string.contacts_filter_customers),
    Vendors(Res.string.contacts_filter_vendors)
}

/**
 * Filter options for active status.
 */
enum class ContactActiveFilter(override val labelRes: StringResource) : FilterOption {
    All(Res.string.contacts_filter_all),
    Active(Res.string.contacts_active),
    Inactive(Res.string.contacts_inactive)
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
     * @property sortOption Current sort order
     * @property roleFilter Current role filter (All, Customers, Vendors)
     * @property activeFilter Current active status filter
     * @property peppolFilter Current Peppol enabled filter (null = all)
     * @property selectedContactId Selected contact for detail view in master-detail layout
     * @property showCreateContactPane Whether the create contact form pane is visible (desktop)
     */
    data class Content(
        val contacts: PaginationState<ContactDto>,
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
    data class ShowError(val error: DokusException) : ContactsAction

    /** Show success message as snackbar/toast */
    data class ShowSuccess(val success: ContactsSuccess) : ContactsAction
}

enum class ContactsSuccess {
    Created,
    Updated,
    Deleted,
}
