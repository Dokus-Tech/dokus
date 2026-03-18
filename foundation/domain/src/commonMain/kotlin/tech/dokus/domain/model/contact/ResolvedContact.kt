package tech.dokus.domain.model.contact

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.ContactId

/**
 * Display-ready contact resolution sent from the backend.
 * Represents the best available contact data for a document,
 * resolved by the backend from linked contacts, suggestions, or extracted data.
 *
 * Priority: Linked > Suggested > Detected > Unknown
 */
@Serializable
sealed interface ResolvedContact {

    /** Real contact linked to the document. */
    @Serializable
    @SerialName("ResolvedContact.Linked")
    data class Linked(
        val contactId: ContactId,
        val name: String,
        val vatNumber: String?,
        val email: String?,
        val avatarPath: String?,
    ) : ResolvedContact

    /** Suggested match from the contact resolution service. */
    @Serializable
    @SerialName("ResolvedContact.Suggested")
    data class Suggested(
        val contactId: ContactId,
        val name: String,
        val vatNumber: String?,
    ) : ResolvedContact

    /** Counterparty data detected from the document, no matching contact found. */
    @Serializable
    @SerialName("ResolvedContact.Detected")
    data class Detected(
        val name: String,
        val vatNumber: String?,
        val iban: String?,
        val address: String?,
    ) : ResolvedContact

    /** No contact data available. */
    @Serializable
    @SerialName("ResolvedContact.Unknown")
    data object Unknown : ResolvedContact
}
