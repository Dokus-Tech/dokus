package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.usecases.SelectOrganizationUseCase
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.platform.Logger

/**
 * Scopes the session to the provided organization by refreshing tokens.
 */
class SelectOrganizationUseCaseImpl(
    private val authRepository: AuthRepository
) : SelectOrganizationUseCase {
    private val logger = Logger.forClass<SelectOrganizationUseCaseImpl>()

    override suspend operator fun invoke(organizationId: OrganizationId): Result<Unit> {
        logger.d { "Selecting organization $organizationId" }
        return authRepository.selectOrganization(organizationId)
    }
}
