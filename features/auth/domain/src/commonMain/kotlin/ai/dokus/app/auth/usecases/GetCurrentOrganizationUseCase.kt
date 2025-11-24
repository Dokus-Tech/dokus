package ai.dokus.app.auth.usecases

import ai.dokus.foundation.domain.model.Organization

/**
 * Returns the organization currently scoped in the user's JWT claims.
 */
interface GetCurrentOrganizationUseCase {
    suspend operator fun invoke(): Result<Organization?>
}
