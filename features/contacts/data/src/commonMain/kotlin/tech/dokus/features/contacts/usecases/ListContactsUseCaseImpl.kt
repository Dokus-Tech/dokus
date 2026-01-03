package tech.dokus.features.contacts.usecases

import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.repository.ContactRemoteDataSource

internal class ListContactsUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : ListContactsUseCase {
    override suspend fun invoke(
        search: String?,
        isActive: Boolean?,
        peppolEnabled: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        return remoteDataSource.listContacts(
            search = search,
            isActive = isActive,
            peppolEnabled = peppolEnabled,
            limit = limit,
            offset = offset
        )
    }
}

internal class ListCustomersUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : ListCustomersUseCase {
    override suspend fun invoke(
        isActive: Boolean,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        return remoteDataSource.listCustomers(
            isActive = isActive,
            limit = limit,
            offset = offset
        )
    }
}

internal class ListVendorsUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : ListVendorsUseCase {
    override suspend fun invoke(
        isActive: Boolean,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        return remoteDataSource.listVendors(
            isActive = isActive,
            limit = limit,
            offset = offset
        )
    }
}

internal class FindContactsByNameUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : FindContactsByNameUseCase {
    override suspend fun invoke(
        query: String,
        limit: Int
    ): Result<List<ContactDto>> {
        return remoteDataSource.listContacts(
            search = query,
            limit = limit,
            offset = 0
        )
    }
}

internal class FindContactsByVatUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : FindContactsByVatUseCase {
    override suspend fun invoke(
        vat: VatNumber,
        limit: Int
    ): Result<List<ContactDto>> {
        val normalizedVat = vat.normalized
        return remoteDataSource.listContacts(
            search = normalizedVat,
            limit = limit,
            offset = 0
        ).map { contacts ->
            contacts.filter { contact ->
                contact.vatNumber?.normalized == normalizedVat
            }
        }
    }
}