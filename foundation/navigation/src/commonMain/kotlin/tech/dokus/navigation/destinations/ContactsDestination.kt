package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Navigation destinations for contacts feature screens.
 */
sealed interface ContactsDestination : NavigationDestination {
    @Serializable
    @SerialName("contacts/create")
    data class CreateContact(
        val prefillCompanyName: String? = null,
        val prefillVat: String? = null,
        val prefillAddress: String? = null,
        val origin: String? = null,
    ) : ContactsDestination

    @Serializable
    @SerialName("contacts/edit")
    data class EditContact(val contactId: String) : ContactsDestination

    @Serializable
    @SerialName("contacts/details")
    data class ContactDetails(val contactId: String) : ContactsDestination
}

@Serializable
enum class ContactCreateOrigin {
    DocumentReview;

    companion object {
        fun fromString(value: String?): ContactCreateOrigin? {
            return entries.find { it.name == value }
        }
    }
}
