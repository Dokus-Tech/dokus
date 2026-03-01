package tech.dokus.features.auth.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.foundation.platform.Logger

/**
 * Returns the tenant that is currently selected in local session state.
 * Falls back to null if no tenant is set.
 */
class GetCurrentTenantUseCaseImpl(
    private val tokenManager: TokenManager,
    private val tenantDataSource: TenantRemoteDataSource
) : GetCurrentTenantUseCase {
    private val logger = Logger.forClass<GetCurrentTenantUseCaseImpl>()

    /**
     * Retrieves the currently selected tenant from local session state via [TokenManager],
     * then fetches the full tenant details from the [TenantRemoteDataSource]. If no tenant
     * is selected yet, returns `null` without making a network request.
     *
     * @return [Result.success] containing:
     *         - [Tenant] with full tenant details if a tenant is selected in the session
     *         - `null` if no tenant is selected yet
     *
     *         [Result.failure] if retrieval failed, which may occur if:
     *         - Network error when fetching tenant details from the remote data source
     *         - Server error from the tenant API
     *         - The tenant ID no longer exists or is inaccessible
     * @see GetCurrentTenantUseCase.invoke for the interface contract
     */
    override suspend operator fun invoke(): Result<Tenant?> {
        val selectedTenantId = tokenManager.getSelectedTenantId()
        if (selectedTenantId == null) {
            logger.d { "No selected tenant in local session state" }
            return Result.success(null)
        }

        return tenantDataSource.getTenant(selectedTenantId)
            .onSuccess { tenant ->
                logger.d { "Loaded current tenant ${tenant.legalName.value} ($selectedTenantId)" }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to load current tenant ($selectedTenantId)" }
            }
            .map { it }
    }
}

internal class WatchCurrentTenantUseCaseImpl(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val tokenStorage: TokenStorage,
) : WatchCurrentTenantUseCase {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun invoke(): Flow<Result<Tenant?>> {
        val tokenChanges = tokenStorage.observeAccessToken()
            .drop(1)
            .map { Unit }

        return merge(refreshTrigger, tokenChanges)
            .flatMapLatest {
                flow {
                    emit(getCurrentTenantUseCase())
                }
            }
    }

    override fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }
}
