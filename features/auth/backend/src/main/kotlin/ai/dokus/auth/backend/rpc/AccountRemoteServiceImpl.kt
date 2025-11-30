@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.auth.DeactivateUserRequest
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.LogoutRequest
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedUserId
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

/**
 * RPC implementation of AccountRemoteService.
 *
 * This service acts as the backend RPC endpoint that clients call.
 * It delegates business logic to AuthService.
 *
 * Error Handling:
 * - Returns plain types on success
 * - Throws exceptions on failure (automatically serialized by RPC framework)
 *
 */
class AccountRemoteServiceImpl(
    private val authService: AuthService,
    private val authInfoProvider: AuthInfoProvider,
    private val userRepository: UserRepository
) : AccountRemoteService {

    private val logger = LoggerFactory.getLogger(AccountRemoteServiceImpl::class.java)

    /**
     * Get the current authenticated user.
     */
    override suspend fun getCurrentUser(): User {
        return authInfoProvider.withAuthInfo {
            val userId = requireAuthenticatedUserId()
            userRepository.findById(userId) ?: throw DokusException.NotAuthenticated("User not found")
        }
    }

    /**
     * Select a tenant and re-issue scoped tokens.
     */
    override suspend fun selectTenant(tenantId: TenantId): LoginResponse {
        logger.debug("RPC: selectTenant called for tenant: ${tenantId.value}")

        return authInfoProvider.withAuthInfo {
            val userId = requireAuthenticatedUserId()
            authService.selectOrganization(userId, tenantId)
                .onFailure { error -> logger.error("RPC: selectTenant failed for user: ${userId.value}", error) }
                .getOrThrow()
        }
    }

    /**
     * Logout user and revoke current session.
     */
    override suspend fun logout(request: LogoutRequest) {
        return authInfoProvider.withAuthInfo {
            logger.debug("RPC: logout called")

            authService.logout(request)
                .onSuccess { logger.info("RPC: logout successful") }
                .onFailure { error -> logger.error("RPC: logout failed", error) }
                .getOrThrow()
        }
    }

    /**
     * Deactivate current user account.
     * Requires authentication - extracts userId from JWT token in coroutine context.
     *
     * Security considerations:
     * - Only the authenticated user can deactivate their own account
     * - The userId is extracted from the JWT token, not from the request
     * - All deactivation attempts are logged for audit purposes
     * - All refresh tokens are revoked to terminate all sessions
     */
    override suspend fun deactivateAccount(request: DeactivateUserRequest) {
        logger.debug("RPC: deactivateAccount called with reason: ${request.reason}")

        try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Deactivating account for authenticated user: ${userId.value}")

            authService.deactivateAccount(userId, request.reason)
                .onSuccess { logger.info("RPC: Account deactivated successfully for user: ${userId.value}") }
                .onFailure { error ->
                    logger.error(
                        "RPC: Account deactivation failed for user: ${userId.value}",
                        error
                    )
                }
                .getOrThrow()
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: deactivateAccount called without authentication")
            throw DokusException.NotAuthenticated("Authentication required to deactivate account")
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in deactivateAccount", e)
            throw e
        }
    }

    /**
     * Resend email verification email.
     * Requires authentication - extracts userId from JWT token in coroutine context.
     */
    override suspend fun resendVerificationEmail() {
        logger.debug("RPC: resendVerificationEmail called")

        try {
            // Extract userId from JWT authentication context
            val userId = requireAuthenticatedUserId()
            logger.debug("Resending verification email for authenticated user: ${userId.value}")

            authService.resendVerificationEmail(userId)
                .onSuccess { logger.info("RPC: Verification email resent successfully for user: ${userId.value}") }
                .onFailure { error ->
                    logger.error(
                        "RPC: Failed to resend verification email for user: ${userId.value}",
                        error
                    )
                }
                .getOrThrow()
        } catch (e: IllegalStateException) {
            // No authentication context available
            logger.error("RPC: resendVerificationEmail called without authentication")
            throw DokusException.NotAuthenticated("Authentication required to resend verification email")
        } catch (e: Exception) {
            logger.error("RPC: Unexpected error in resendVerificationEmail", e)
            throw e
        }
    }
}
