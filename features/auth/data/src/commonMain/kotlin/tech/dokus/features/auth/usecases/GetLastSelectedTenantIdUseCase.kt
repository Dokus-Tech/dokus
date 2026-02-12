package tech.dokus.features.auth.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.features.auth.storage.TokenStorage

class GetLastSelectedTenantIdUseCaseImpl(
    private val tokenStorage: TokenStorage
) : GetLastSelectedTenantIdUseCase {
    override suspend fun invoke(): TenantId? = tokenStorage.getLastSelectedTenantId()
}
