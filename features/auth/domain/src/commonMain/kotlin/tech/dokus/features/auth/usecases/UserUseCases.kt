package tech.dokus.features.auth.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.User
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.SessionDto

/**
 * Use case for retrieving current session/bootstrap payload.
 */
interface GetAccountMeUseCase {
    suspend operator fun invoke(): Result<AccountMeResponse>
}

interface RefreshSessionNowUseCase {
    suspend operator fun invoke(): Result<Unit>
}

/**
 * Use case for retrieving current user.
 */
interface GetCurrentUserUseCase {
    suspend operator fun invoke(): Result<User>
}

interface CreateFirmUseCase {
    suspend operator fun invoke(request: CreateFirmRequest): Result<FirmWorkspaceSummary>
}

/**
 * Use case for listing console clients for bookkeepers.
 */
interface ListConsoleClientsUseCase {
    suspend operator fun invoke(firmId: FirmId): Result<List<ConsoleClientSummary>>
}

interface ListConsoleClientDocumentsUseCase {
    suspend operator fun invoke(
        firmId: FirmId,
        tenantId: TenantId,
        page: Int = 0,
        limit: Int = 20,
    ): Result<PaginatedResponse<DocumentListItemDto>>
}

interface GetConsoleClientDocumentUseCase {
    suspend operator fun invoke(
        firmId: FirmId,
        tenantId: TenantId,
        documentId: String,
    ): Result<DocumentDetailDto>
}

/**
 * Reactive stream for the current user profile.
 */
interface WatchCurrentUserUseCase {
    operator fun invoke(): Flow<Result<User>>
    fun refresh()
}

/**
 * Use case for updating user profile.
 */
interface UpdateProfileUseCase {
    suspend operator fun invoke(firstName: Name?, lastName: Name?): Result<User>
}

interface UploadUserAvatarUseCase {
    suspend operator fun invoke(
        userId: UserId,
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Thumbnail>
}

interface DeleteUserAvatarUseCase {
    suspend operator fun invoke(userId: UserId): Result<Unit>
}

/**
 * Use case for changing current account password.
 */
interface ChangePasswordUseCase {
    suspend operator fun invoke(currentPassword: Password, newPassword: Password): Result<Unit>
}

/**
 * Use case for listing active user sessions.
 */
interface ListSessionsUseCase {
    suspend operator fun invoke(): Result<List<SessionDto>>
}

/**
 * Use case for revoking one specific session.
 */
interface RevokeSessionUseCase {
    suspend operator fun invoke(sessionId: SessionId): Result<Unit>
}

/**
 * Use case for revoking all sessions except current.
 */
interface RevokeOtherSessionsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
