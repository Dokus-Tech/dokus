package ai.dokus.app.contacts.usecases

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.ContactActivitySummary
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.ContactMergeResult
import tech.dokus.domain.model.UpdateContactPeppolRequest

/**
 * Use case for getting contact activity summary (invoices, bills, expenses).
 */
interface GetContactActivityUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<ContactActivitySummary>
}

/**
 * Use case for updating Peppol settings for a contact.
 */
interface UpdateContactPeppolUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        request: UpdateContactPeppolRequest
    ): Result<ContactDto>
}

/**
 * Use case for merging two contacts.
 */
interface MergeContactsUseCase {
    suspend operator fun invoke(
        sourceContactId: ContactId,
        targetContactId: ContactId
    ): Result<ContactMergeResult>
}
