package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

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
        val id: UserId,
        val email: Email,
        val firstName: Name?,
        val lastName: Name?,
        val emailVerified: Boolean = false,
        val isActive: Boolean = true,
        val lastLoginAt: LocalDateTime? = null,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
        val memberships: List<OrganizationMembership> = emptyList()
    ) {
        val fullName: String
            get() = listOfNotNull(firstName, lastName).joinToString(" ").trim()
                .ifEmpty { email.value }

        /**
         * Get a user's role in a specific organization
         */
        fun getRoleIn(organizationId: OrganizationId): UserRole? =
            memberships.find { it.organizationId == organizationId }?.role

        /**
         * Check if user has access to a specific organization
         */
        fun hasAccessTo(organizationId: OrganizationId): Boolean =
            memberships.any { it.organizationId == organizationId && it.isActive }
    }

    /**
     * Summary user DTO with minimal fields
     * Used for listings and references
     */
    @Serializable
    data class Summary(
        val id: UserId,
        val email: Email,
        val fullName: Name,
        val role: UserRole? = null
    )

    /**
     * Public user DTO without sensitive information
     * Used for public APIs and external integrations
     */
    @Serializable
    data class Public(
        val id: UserId,
        val fullName: Name,
        val email: Email
    )
}
