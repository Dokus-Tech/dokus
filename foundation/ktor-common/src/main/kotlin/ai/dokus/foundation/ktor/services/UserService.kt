package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.BusinessUser
import kotlinx.datetime.Instant
import kotlinx.rpc.RPC

@RPC
interface UserService {
    /**
     * Registers a new user for a tenant
     *
     * @param tenantId The tenant ID this user belongs to
     * @param email The user's email address (must be unique)
     * @param password The user's plain text password (will be hashed)
     * @param firstName The user's first name (optional)
     * @param lastName The user's last name (optional)
     * @param role The user's role (defaults to Viewer)
     * @return The created user
     * @throws IllegalArgumentException if email already exists
     */
    suspend fun register(
        tenantId: TenantId,
        email: String,
        password: String,
        firstName: String? = null,
        lastName: String? = null,
        role: UserRole = UserRole.Viewer
    ): BusinessUser

    /**
     * Finds a user by their unique ID
     *
     * @param id The user's unique identifier
     * @return The user if found, null otherwise
     */
    suspend fun findById(id: UserId): BusinessUser?

    /**
     * Finds a user by their email address
     *
     * @param email The user's email address
     * @return The user if found, null otherwise
     */
    suspend fun findByEmail(email: String): BusinessUser?

    /**
     * Lists all users belonging to a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param activeOnly If true, only returns active users (defaults to true)
     * @return List of users for the tenant
     */
    suspend fun listByTenant(tenantId: TenantId, activeOnly: Boolean = true): List<BusinessUser>

    /**
     * Updates a user's role
     * Requires Owner or Admin permissions
     *
     * @param userId The user's unique identifier
     * @param newRole The new role to assign
     * @throws IllegalArgumentException if user not found
     */
    suspend fun updateRole(userId: UserId, newRole: UserRole)

    /**
     * Updates a user's profile information
     *
     * @param userId The user's unique identifier
     * @param firstName The user's first name (optional)
     * @param lastName The user's last name (optional)
     * @throws IllegalArgumentException if user not found
     */
    suspend fun updateProfile(userId: UserId, firstName: String?, lastName: String?)

    /**
     * Deactivates a user account
     * User can no longer log in but data is preserved
     *
     * @param userId The user's unique identifier
     * @throws IllegalArgumentException if user not found
     */
    suspend fun deactivate(userId: UserId)

    /**
     * Reactivates a previously deactivated user account
     *
     * @param userId The user's unique identifier
     * @throws IllegalArgumentException if user not found
     */
    suspend fun reactivate(userId: UserId)

    /**
     * Updates the user's password
     *
     * @param userId The user's unique identifier
     * @param newPassword The new plain text password (will be hashed)
     * @throws IllegalArgumentException if user not found
     */
    suspend fun updatePassword(userId: UserId, newPassword: String)

    /**
     * Records a user's login timestamp
     *
     * @param userId The user's unique identifier
     * @param loginTime The timestamp of the login
     */
    suspend fun recordLogin(userId: UserId, loginTime: Instant)

    /**
     * Sets up MFA (Multi-Factor Authentication) for a user
     *
     * @param userId The user's unique identifier
     * @param mfaSecret The TOTP secret (must be encrypted before storage)
     * @throws IllegalArgumentException if user not found
     */
    suspend fun setupMfa(userId: UserId, mfaSecret: String)

    /**
     * Removes MFA from a user's account
     *
     * @param userId The user's unique identifier
     * @throws IllegalArgumentException if user not found
     */
    suspend fun removeMfa(userId: UserId)

    /**
     * Verifies a user's password
     *
     * @param email The user's email address
     * @param password The plain text password to verify
     * @return The user if credentials are valid, null otherwise
     */
    suspend fun verifyCredentials(email: String, password: String): BusinessUser?
}
