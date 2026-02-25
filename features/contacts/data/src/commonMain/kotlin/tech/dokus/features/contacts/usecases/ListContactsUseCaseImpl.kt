package tech.dokus.features.contacts.usecases

import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.repository.ContactRemoteDataSource
import tech.dokus.features.contacts.usecases.LookupContactsUseCase

internal class ListContactsUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : ListContactsUseCase {
    override suspend fun invoke(
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        // NOTE: peppolEnabled removed - PEPPOL status is in PeppolDirectoryCacheTable
        return remoteDataSource.listContacts(
            isActive = isActive,
            limit = limit,
            offset = offset
        )
    }
}

internal class LookupContactsUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : LookupContactsUseCase {
    override suspend fun invoke(
        query: String,
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        return remoteDataSource.lookupContacts(
            query = query,
            isActive = isActive,
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
    private val lookupContacts: LookupContactsUseCase
) : FindContactsByNameUseCase {
    override suspend fun invoke(
        query: String,
        limit: Int
    ): Result<List<ContactDto>> {
        return lookupContacts(
            query = query,
            isActive = true,
            limit = limit,
            offset = 0
        )
    }
}

internal class FindContactsByVatUseCaseImpl(
    private val lookupContacts: LookupContactsUseCase
) : FindContactsByVatUseCase {
    override suspend fun invoke(
        vat: VatNumber,
        limit: Int
    ): Result<List<ContactDto>> {
        val normalizedVat = vat.normalized
        return lookupContacts(
            query = normalizedVat,
            isActive = true,
            limit = limit,
            offset = 0
        ).map { contacts ->
            contacts.filter { contact ->
                contact.vatNumber?.normalized == normalizedVat
            }
        }
    }
}
