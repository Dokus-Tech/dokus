package ai.dokus.app.contacts.usecases

import ai.dokus.foundation.domain.model.ContactDto

/**
 * Use case for listing contacts with optional filters.
 */
interface ListContactsUseCase {
    suspend operator fun invoke(
        search: String? = null,
        isActive: Boolean? = null,
        peppolEnabled: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>
}

/**
 * Use case for listing customer contacts only.
 */
interface ListCustomersUseCase {
    suspend operator fun invoke(
        isActive: Boolean = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>
}

/**
 * Use case for listing vendor contacts only.
 */
interface ListVendorsUseCase {
    suspend operator fun invoke(
        isActive: Boolean = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>
}
