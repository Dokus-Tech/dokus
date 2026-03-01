package tech.dokus.features.auth.gateway

import kotlinx.coroutines.flow.StateFlow
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.LegalName
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.LoginRequest
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.domain.model.auth.SessionDto

/**
 * Gateway for authentication workflows and token-scoped operations.
 */
interface AuthGateway {
    val isAuthenticated: StateFlow<Boolean>

    suspend fun initialize()

    suspend fun login(request: LoginRequest): Result<Unit>

    suspend fun register(request: RegisterRequest): Result<Unit>

    suspend fun selectTenant(tenantId: TenantId): Result<Unit>

    @Suppress("LongParameterList") // Tenant creation requires full parameter set
    suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: SubscriptionTier,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): Result<Tenant>

    suspend fun hasFreelancerTenant(): Result<Boolean>

    suspend fun getAccountMe(): Result<AccountMeResponse>

    suspend fun getCurrentUser(): Result<User>

    suspend fun updateProfile(firstName: Name?, lastName: Name?): Result<User>

    suspend fun logout()

    suspend fun requestPasswordReset(email: Email): Result<Unit>

    suspend fun resetPassword(resetToken: String, newPassword: String): Result<Unit>

    suspend fun verifyEmail(token: String): Result<Unit>

    suspend fun resendVerificationEmail(): Result<Unit>

    suspend fun changePassword(currentPassword: Password, newPassword: Password): Result<Unit>

    suspend fun listSessions(): Result<List<SessionDto>>

    suspend fun revokeSession(sessionId: SessionId): Result<Unit>

    suspend fun revokeOtherSessions(): Result<Unit>
}
