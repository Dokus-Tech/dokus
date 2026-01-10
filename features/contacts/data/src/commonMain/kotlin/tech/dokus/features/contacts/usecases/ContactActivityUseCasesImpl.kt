package tech.dokus.features.contacts.usecases

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.features.contacts.repository.ContactRemoteDataSource

internal class GetContactActivityUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : GetContactActivityUseCase {
    override suspend fun invoke(contactId: ContactId): Result<ContactActivitySummary> {
        return remoteDataSource.getContactActivity(contactId)
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
