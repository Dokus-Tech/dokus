package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.domain.OrganizationRemoteService
import ai.dokus.app.auth.usecases.GetCurrentOrganizationUseCase
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.platform.Logger

/**
 * Returns the organization that is currently selected in the JWT claims.
 * Falls back to null if no organization is set.
 */
class GetCurrentOrganizationUseCaseImpl(
    private val tokenManager: TokenManager,
    private val organizationRemoteService: OrganizationRemoteService
) : GetCurrentOrganizationUseCase {
    private val logger = Logger.forClass<GetCurrentOrganizationUseCaseImpl>()

    override suspend operator fun invoke(): Result<Organization?> = runCatching {
        val claims = tokenManager.getCurrentClaims()
        val organizationScope = claims?.organization
        if (organizationScope == null) {
            logger.d { "No organization present in JWT claims" }
            return@runCatching null
        }

        organizationRemoteService.getOrganization(organizationScope.organizationId).also { organization ->
            logger.d { "Loaded current organization ${organization.legalName.value}" }
        }
    }.onFailure { error ->
        logger.e(error) { "Failed to load current organization from claims" }
    }
}
