package tech.dokus.domain.model.contact

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.ContactId

object ContactStreamEventNames {
    const val Changed = "contact_changed"
}

@Serializable
data class ContactChangedEventDto(
    val contactId: ContactId,
    val reason: String? = null,
)
