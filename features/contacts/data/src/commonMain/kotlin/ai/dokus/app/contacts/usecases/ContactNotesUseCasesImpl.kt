package ai.dokus.app.contacts.usecases

import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest

internal class ListContactNotesUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : ListContactNotesUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        limit: Int,
        offset: Int
    ): Result<List<ContactNoteDto>> {
        return remoteDataSource.listNotes(contactId, limit, offset)
    }
}

internal class CreateContactNoteUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : CreateContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        request: CreateContactNoteRequest
    ): Result<ContactNoteDto> {
        return remoteDataSource.createNote(contactId, request)
    }
}

internal class UpdateContactNoteUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : UpdateContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ): Result<ContactNoteDto> {
        return remoteDataSource.updateNote(contactId, noteId, request)
    }
}

internal class DeleteContactNoteUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : DeleteContactNoteUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        noteId: ContactNoteId
    ): Result<Unit> {
        return remoteDataSource.deleteNote(contactId, noteId)
    }
}
