package ai.dokus.app.contacts.usecases

import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import ai.dokus.foundation.domain.ids.ContactId
import tech.dokus.domain.model.ContactActivitySummary
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.ContactMergeResult
import tech.dokus.domain.model.UpdateContactPeppolRequest

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
