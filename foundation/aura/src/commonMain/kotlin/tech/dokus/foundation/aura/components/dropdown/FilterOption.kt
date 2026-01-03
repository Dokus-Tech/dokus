package tech.dokus.foundation.aura.components.dropdown

import org.jetbrains.compose.resources.StringResource

/**
 * Interface that filter enum types must implement to work with [PFilterDropdown].
 *
 * This enables type-safe generic dropdown behavior for filtering UI components.
 * Filter enums implementing this interface can be used with the generic PFilterDropdown
 * component, which displays the [displayName] to users.
 *
 * Example usage:
 * ```kotlin
 * enum class ContactRoleFilter(override val labelRes: StringResource) : FilterOption {
 *     All(Res.string.contacts_filter_all),
 *     Customers(Res.string.contacts_filter_customers),
 *     Vendors(Res.string.contacts_filter_vendors)
 * }
 * ```
 */
interface FilterOption {
    /**
     * The string resource to display in the dropdown UI.
     */
    val labelRes: StringResource
}
