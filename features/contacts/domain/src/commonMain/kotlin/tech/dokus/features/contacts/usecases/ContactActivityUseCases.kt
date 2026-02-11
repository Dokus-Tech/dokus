package tech.dokus.features.contacts.usecases

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactMergeResult

/**
 * Use case for getting contact activity summary (invoices, inbound invoices, expenses).
 */
interface GetContactActivityUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<ContactActivitySummary>
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
