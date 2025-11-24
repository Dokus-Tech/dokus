package ai.dokus.app.auth.usecases

import ai.dokus.foundation.domain.ids.OrganizationId

/**
 * Scopes the user's session to the provided organization.
 */
interface SelectOrganizationUseCase {
    suspend operator fun invoke(organizationId: OrganizationId): Result<Unit>
}
