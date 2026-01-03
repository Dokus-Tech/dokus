package tech.dokus.features.contacts.usecases

import tech.dokus.domain.model.contact.ContactDto

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
 * Use case for finding contacts by name.
 */
interface FindContactsByNameUseCase {
    suspend operator fun invoke(
        query: String,
        limit: Int = 50
    ): Result<List<ContactDto>>
}

/**
 * Use case for finding contacts by VAT number.
 */
interface FindContactsByVatUseCase {
    suspend operator fun invoke(
        vat: String,
        limit: Int = 50
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
