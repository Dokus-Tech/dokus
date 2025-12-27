package ai.dokus.app.contacts.usecases

import ai.dokus.app.contacts.repository.ContactRemoteDataSource
import tech.dokus.domain.model.ContactDto

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
