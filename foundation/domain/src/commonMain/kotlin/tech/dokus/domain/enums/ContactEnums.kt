package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

// ============================================================================
// CONTACT ENUMS
// ============================================================================

/**
 * Type of address for contacts.
 * Each contact can have multiple addresses of different types.
 */
@Serializable
enum class AddressType(override val dbValue: String) : DbEnum {
    @SerialName("registered")
    Registered("registered"),

    @SerialName("billing")
    Billing("billing"),

    @SerialName("shipping")
    Shipping("shipping"),

    @SerialName("other")
    Other("other")
}
