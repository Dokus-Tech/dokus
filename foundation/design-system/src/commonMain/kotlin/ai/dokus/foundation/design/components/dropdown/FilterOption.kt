package ai.dokus.foundation.design.components.dropdown

/**
 * Interface that filter enum types must implement to work with [PFilterDropdown].
 *
 * This enables type-safe generic dropdown behavior for filtering UI components.
 * Filter enums implementing this interface can be used with the generic PFilterDropdown
 * component, which displays the [displayName] to users.
 *
 * Example usage:
 * ```kotlin
 * enum class ContactRoleFilter(override val displayName: String) : FilterOption {
 *     All("All"),
 *     Customers("Customers"),
 *     Vendors("Vendors")
 * }
 * ```
 */
interface FilterOption {
    /**
     * The human-readable name to display in the dropdown UI.
     */
    val displayName: String
}
