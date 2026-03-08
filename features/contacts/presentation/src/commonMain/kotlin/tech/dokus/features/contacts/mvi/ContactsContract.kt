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
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.dropdown.FilterOption

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
data class ContactsState(
    val contacts: DokusState<PaginationState<ContactDto>>,
    val sortOption: ContactSortOption = ContactSortOption.Default,
    val roleFilter: ContactRoleFilter = ContactRoleFilter.All,
    val activeFilter: ContactActiveFilter = ContactActiveFilter.All,
    val peppolFilter: Boolean? = null,
    val selectedContactId: ContactId? = null,
    val showCreateContactPane: Boolean = false,
) : MVIState {
    companion object {
        const val PAGE_SIZE = 20
        val initial by lazy {
            ContactsState(contacts = DokusState.loading())
        }
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
