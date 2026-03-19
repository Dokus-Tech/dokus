package tech.dokus.domain.model.contact

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.ContactId

/**
 * Lightweight contact suggestion for the contact picker.
 * Contains only the fields needed to display a suggestion chip.
 */
@Serializable
data class ContactSuggestionDto(
    val contactId: ContactId,
    val name: String,
    val vatNumber: String?,
)
