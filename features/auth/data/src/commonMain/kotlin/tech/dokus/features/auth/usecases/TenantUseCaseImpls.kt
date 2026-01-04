package tech.dokus.features.auth.usecases

import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.datasource.TenantRemoteDataSource

internal class ListMyTenantsUseCaseImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : ListMyTenantsUseCase {
    override suspend fun invoke(): Result<List<Tenant>> {
        return tenantRemoteDataSource.listMyTenants()
    }
}

internal class GetInvoiceNumberPreviewUseCaseImpl(
    private val tenantRemoteDataSource: TenantRemoteDataSource
) : GetInvoiceNumberPreviewUseCase {
    override suspend fun invoke(): Result<String> {
        return tenantRemoteDataSource.getInvoiceNumberPreview()
    }
}
