package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Navigation destinations for contacts feature screens.
 */
sealed interface ContactsDestination : NavigationDestination {
    @Serializable
    @SerialName("contacts/create")
    data object CreateContact : ContactsDestination

    @Serializable
    @SerialName("contacts/edit")
    data class EditContact(val contactId: String) : ContactsDestination

    @Serializable
    @SerialName("contacts/details")
    data class ContactDetails(val contactId: String) : ContactsDestination
}
