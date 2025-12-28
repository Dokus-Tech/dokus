package ai.dokus.app.contacts.usecases

import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactRequest

internal class GetContactUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : GetContactUseCase {
    override suspend fun invoke(contactId: ContactId): Result<ContactDto> {
        return remoteDataSource.getContact(contactId)
    }
}

internal class CreateContactUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : CreateContactUseCase {
    override suspend fun invoke(request: CreateContactRequest): Result<ContactDto> {
        return remoteDataSource.createContact(request)
    }
}

internal class UpdateContactUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : UpdateContactUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        request: UpdateContactRequest
    ): Result<ContactDto> {
        return remoteDataSource.updateContact(contactId, request)
    }
}

internal class DeleteContactUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : DeleteContactUseCase {
    override suspend fun invoke(contactId: ContactId): Result<Unit> {
        return remoteDataSource.deleteContact(contactId)
    }
}
