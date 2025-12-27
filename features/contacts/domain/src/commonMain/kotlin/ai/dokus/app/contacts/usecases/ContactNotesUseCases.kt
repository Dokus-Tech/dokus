package ai.dokus.app.contacts.usecases

import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import tech.dokus.domain.model.ContactNoteDto
import tech.dokus.domain.model.CreateContactNoteRequest
import tech.dokus.domain.model.UpdateContactNoteRequest

/**
 * Use case for listing notes for a contact.
 */
interface ListContactNotesUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactNoteDto>>
}

/**
 * Use case for creating a new note on a contact.
 */
interface CreateContactNoteUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        request: CreateContactNoteRequest
    ): Result<ContactNoteDto>
}

/**
 * Use case for updating an existing note.
 */
interface UpdateContactNoteUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ): Result<ContactNoteDto>
}

/**
 * Use case for deleting a note.
 */
interface DeleteContactNoteUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId
    ): Result<Unit>
}
