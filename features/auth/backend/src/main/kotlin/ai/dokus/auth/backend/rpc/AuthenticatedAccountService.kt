package ai.dokus.auth.backend.rpc

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.security.AuthContext
import ai.dokus.auth.backend.security.AuthenticationInfo
import ai.dokus.foundation.domain.model.auth.*
import kotlinx.coroutines.withContext

/**
 * Wrapper for AccountRemoteService that injects authentication context.
 * This allows authenticated methods to access user information via coroutine context.
 */
class AuthenticatedAccountService(
    private val delegate: AccountRemoteService,
    private val authInfoProvider: suspend () -> AuthenticationInfo?
) : AccountRemoteService {

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return delegate.login(request)
    }

    override suspend fun register(request: RegisterRequest): Result<LoginResponse> {
        return delegate.register(request)
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): Result<LoginResponse> {
        return delegate.refreshToken(request)
    }

    override suspend fun logout(request: LogoutRequest): Result<Unit> {
        return withAuthContextIfAvailable {
            delegate.logout(request)
        }
    }

    override suspend fun requestPasswordReset(email: String): Result<Unit> {
        return delegate.requestPasswordReset(email)
    }

    override suspend fun resetPassword(resetToken: String, request: ResetPasswordRequest): Result<Unit> {
        return delegate.resetPassword(resetToken, request)
    }

    override suspend fun deactivateAccount(request: DeactivateUserRequest): Result<Unit> {
        return withAuthContextIfAvailable {
            delegate.deactivateAccount(request)
        }
    }

    override suspend fun verifyEmail(token: String): Result<Unit> {
        return delegate.verifyEmail(token)
    }

    override suspend fun resendVerificationEmail(): Result<Unit> {
        return withAuthContextIfAvailable {
            delegate.resendVerificationEmail()
        }
    }

    private suspend fun <T> withAuthContextIfAvailable(block: suspend () -> T): T {
        val authInfo = authInfoProvider()
        return if (authInfo != null) {
            withContext(AuthContext(authInfo)) {
                block()
            }
        } else {
            block()
        }
    }
}
