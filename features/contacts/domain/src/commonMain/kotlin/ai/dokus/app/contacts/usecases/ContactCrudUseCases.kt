package ai.dokus.app.contacts.usecases

import ai.dokus.foundation.domain.ids.ContactId
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.CreateContactRequest
import tech.dokus.domain.model.UpdateContactRequest

/**
 * Use case for getting a single contact by ID.
 */
interface GetContactUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<ContactDto>
}

/**
 * Use case for creating a new contact.
 */
interface CreateContactUseCase {
    suspend operator fun invoke(request: CreateContactRequest): Result<ContactDto>
}

/**
 * Use case for updating an existing contact.
 */
interface UpdateContactUseCase {
    suspend operator fun invoke(
        contactId: ContactId,
        request: UpdateContactRequest
    ): Result<ContactDto>
}

/**
 * Use case for deleting a contact.
 */
interface DeleteContactUseCase {
    suspend operator fun invoke(contactId: ContactId): Result<Unit>
}
