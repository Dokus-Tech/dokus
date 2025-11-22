package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.OrganizationMembership
import ai.dokus.foundation.domain.model.UserInOrganization
import kotlinx.datetime.Instant
import kotlinx.rpc.annotations.Rpc
import kotlin.time.ExperimentalTime

@Rpc
interface UserService {
    /**
     * Registers a new user and adds them to an organization with a role
     */
    suspend fun register(
        organizationId: OrganizationId,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        role: UserRole = UserRole.Viewer
    ): User

    /**
     * Finds a user by their unique ID
     */
    suspend fun findById(id: UserId): User?

    /**
     * Finds a user by their email address
     */
    suspend fun findByEmail(email: String): User?

    /**
     * Lists all users belonging to an organization
     */
    suspend fun listByOrganization(organizationId: OrganizationId, activeOnly: Boolean = true): List<UserInOrganization>

    /**
     * Gets all organizations a user belongs to
     */
    suspend fun getUserOrganizations(userId: UserId): List<OrganizationMembership>

    /**
     * Gets user's membership in a specific organization
     */
    suspend fun getMembership(userId: UserId, organizationId: OrganizationId): OrganizationMembership?

    /**
     * Adds a user to an organization with a role
     */
    suspend fun addToOrganization(userId: UserId, organizationId: OrganizationId, role: UserRole)

    /**
     * Updates a user's role in an organization
     */
    suspend fun updateRole(userId: UserId, organizationId: OrganizationId, newRole: UserRole)

    /**
     * Removes a user from an organization (deactivates membership)
     */
    suspend fun removeFromOrganization(userId: UserId, organizationId: OrganizationId)

    /**
     * Updates a user's profile information
     */
    suspend fun updateProfile(userId: UserId, firstName: String?, lastName: String?)

    /**
     * Deactivates a user account (soft delete)
     */
    suspend fun deactivate(userId: UserId, reason: String? = null)

    /**
     * Reactivates a previously deactivated user account
     */
    suspend fun reactivate(userId: UserId)

    /**
     * Updates the user's password
     */
    suspend fun updatePassword(userId: UserId, newPassword: String)

    /**
     * Records a user's login timestamp
     */
    @OptIn(ExperimentalTime::class)
    suspend fun recordLogin(userId: UserId, loginTime: Instant)

    /**
     * Verifies a user's password
     */
    suspend fun verifyCredentials(email: String, password: String): User?
}
