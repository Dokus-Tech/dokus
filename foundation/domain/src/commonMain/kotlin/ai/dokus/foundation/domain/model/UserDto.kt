package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.UserStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * User Data Transfer Object
 *
 * This model is used for transferring user data between services via RPC
 * and for API responses. It does not include sensitive data like password hashes.
 */
sealed class UserDto {

    /**
     * Full user DTO with all fields
     * Used for authenticated user context in microservices
     */
    @Serializable
    data class Full(
        val id: BusinessUserId,
        val tenantId: TenantId,
        val email: Email,
        val firstName: String?,
        val lastName: String?,
        val role: UserRole,
        val emailVerified: Boolean = false,
        val isActive: Boolean = true,
        val lastLoginAt: LocalDateTime? = null,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        val fullName: String
            get() = listOfNotNull(firstName, lastName).joinToString(" ").trim()
                .ifEmpty { email.value }
    }

    /**
     * Summary user DTO with minimal fields
     * Used for listings and references
     */
    @Serializable
    data class Summary(
        val id: BusinessUserId,
        val email: Email,
        val fullName: String,
        val role: UserRole
    )

    /**
     * Public user DTO without sensitive information
     * Used for public APIs and external integrations
     */
    @Serializable
    data class Public(
        val id: BusinessUserId,
        val fullName: String,
        val email: Email
    )

    companion object {
        /**
         * Create Full DTO from JwtPrincipal
         * Useful for reconstructing user context from JWT claims
         */
        @OptIn(ExperimentalTime::class)
        fun fromJwtPrincipal(principal: JwtPrincipal): Full {
            val now = Clock.System.now()
            return Full(
                id = BusinessUserId.parse(principal.userId),
                tenantId = TenantId.parse(principal.tenantId),
                email = Email(principal.email),
                firstName = principal.firstName,
                lastName = principal.lastName,
                role = principal.roles.firstOrNull()?.let { UserRole.valueOf(it) } ?: UserRole.Viewer,
                emailVerified = true,
                isActive = true,
                lastLoginAt = null,
                createdAt = now.toLocalDateTime(TimeZone.UTC),
                updatedAt = now.toLocalDateTime(TimeZone.UTC)
            )
        }
    }
}
