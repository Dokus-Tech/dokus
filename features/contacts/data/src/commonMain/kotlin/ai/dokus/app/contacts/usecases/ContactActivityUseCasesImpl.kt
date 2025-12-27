package ai.dokus.app.contacts.usecases

import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.domain.model.contact.UpdateContactPeppolRequest

internal class GetContactActivityUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : GetContactActivityUseCase {
    override suspend fun invoke(contactId: ContactId): Result<ContactActivitySummary> {
        return remoteDataSource.getContactActivity(contactId)
    }
}

internal class UpdateContactPeppolUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : UpdateContactPeppolUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        request: UpdateContactPeppolRequest
    ): Result<ContactDto> {
        return remoteDataSource.updateContactPeppol(contactId, request)
    }
}

internal class MergeContactsUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : MergeContactsUseCase {
    override suspend fun invoke(
        sourceContactId: ContactId,
        targetContactId: ContactId
    ): Result<ContactMergeResult> {
        return remoteDataSource.mergeContacts(sourceContactId, targetContactId)
    }
}
