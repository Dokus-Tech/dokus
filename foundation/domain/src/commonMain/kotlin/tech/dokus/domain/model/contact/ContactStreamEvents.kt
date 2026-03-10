package tech.dokus.domain.model.contact

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.ContactId

object ContactStreamEventNames {
    const val Changed = "contact_changed"
}

object ContactStreamEventReasons {
    const val ContactUpdated = "contact_updated"
    const val ProfileUpdated = "profile_updated"
    const val NoteCreated = "note_created"
    const val NoteUpdated = "note_updated"
    const val NoteDeleted = "note_deleted"
}

@Serializable
data class ContactChangedEventDto(
    val contactId: ContactId,
    val reason: String? = null,
)
