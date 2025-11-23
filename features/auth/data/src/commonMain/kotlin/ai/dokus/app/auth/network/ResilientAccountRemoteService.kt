package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.network.resilient.AuthResilientService

/**
 * Resilient wrapper for AccountRemoteService that retries requests using ResilientDelegate.
 * Mirrors the approach used by ResilientOrganizationRemoteService.
 */
class ResilientAccountRemoteService(
    serviceProvider: () -> AccountRemoteService,
    tokenManager: ai.dokus.foundation.domain.asbtractions.TokenManager,
    authManager: ai.dokus.foundation.domain.asbtractions.AuthManager
) : AccountRemoteService, AuthResilientService<AccountRemoteService>(serviceProvider, tokenManager, authManager) {

    private suspend fun <R> withRetry(block: suspend (AccountRemoteService) -> R): R =
        authCall(block)

    override suspend fun selectOrganization(organizationId: OrganizationId): LoginResponse =
        withRetry { it.selectOrganization(organizationId) }

    override suspend fun logout(request: LogoutRequest) =
        withRetry { it.logout(request) }

    override suspend fun deactivateAccount(request: DeactivateUserRequest) =
        withRetry { it.deactivateAccount(request) }

    override suspend fun resendVerificationEmail() =
        withRetry { it.resendVerificationEmail() }
}
