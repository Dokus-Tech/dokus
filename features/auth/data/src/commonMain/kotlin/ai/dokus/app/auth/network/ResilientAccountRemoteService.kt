package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.invoke

/**
 * Resilient wrapper for AccountRemoteService that retries requests using RetryResilientDelegate.
 * Mirrors the approach used by ResilientOrganizationRemoteService.
 */
class ResilientAccountRemoteService(
    private val delegate: RemoteServiceDelegate<AccountRemoteService>,
) : AccountRemoteService {

    override suspend fun selectOrganization(organizationId: OrganizationId): LoginResponse {
        return delegate { it.selectOrganization(organizationId) }
    }

    override suspend fun logout(request: LogoutRequest) {
        return delegate { it.logout(request) }
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest) {
        return delegate { it.deactivateAccount(request) }
    }

    override suspend fun resendVerificationEmail() {
        return delegate { it.resendVerificationEmail() }
    }
}
