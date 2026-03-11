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
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.CreateFirmResponse
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

    suspend fun refreshSessionNow(): Result<Unit>

    suspend fun createFirm(request: CreateFirmRequest): Result<CreateFirmResponse>

    suspend fun listConsoleClients(firmId: FirmId): Result<List<ConsoleClientSummary>>

    suspend fun listConsoleClientDocuments(
        firmId: FirmId,
        tenantId: TenantId,
        page: Int = 0,
        limit: Int = 20,
    ): Result<PaginatedResponse<DocumentListItemDto>>

    suspend fun getConsoleClientDocument(
        firmId: FirmId,
        tenantId: TenantId,
        documentId: String,
    ): Result<DocumentDetailDto>

    suspend fun getCurrentUser(): Result<User>

    suspend fun updateProfile(firstName: Name?, lastName: Name?): Result<User>

    suspend fun uploadUserAvatar(
        userId: UserId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Thumbnail>

    suspend fun deleteUserAvatar(userId: UserId): Result<Unit>

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
