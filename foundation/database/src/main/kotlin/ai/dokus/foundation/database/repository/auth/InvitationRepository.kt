package ai.dokus.foundation.database.repository.auth

import ai.dokus.foundation.database.tables.auth.TenantInvitationsTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import tech.dokus.domain.Email
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.foundation.ktor.database.dbQuery
import tech.dokus.foundation.ktor.utils.loggerFor
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.security.SecureRandom
import java.util.Base64
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Repository for managing tenant invitations.
 * Handles creation, lookup, acceptance, and cancellation of invitations.
 */
@OptIn(ExperimentalUuidApi::class)
class InvitationRepository {
    private val logger = loggerFor()

    /**
     * Create a new invitation for a user to join a tenant.
     *
     * @param tenantId Target tenant
     * @param email Invited user's email
     * @param role Role to assign when accepted
     * @param invitedBy User who created the invitation
     * @param expiresAt When the invitation expires
     * @return The created invitation ID
     * @throws IllegalArgumentException if a pending invitation already exists for this email/tenant
     */
    suspend fun create(
        tenantId: TenantId,
        email: Email,
        role: UserRole,
        invitedBy: UserId,
        expiresAt: Instant
    ): InvitationId = dbQuery {
        // Check for existing pending invitation
        val existing = TenantInvitationsTable
            .selectAll()
            .where {
                (TenantInvitationsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (TenantInvitationsTable.email eq email.value) and
                        (TenantInvitationsTable.status eq InvitationStatus.Pending)
            }
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException("Pending invitation already exists for ${email.value}")
        }

        val token = generateSecureToken()

        val invitationId = TenantInvitationsTable.insertAndGetId {
            it[TenantInvitationsTable.tenantId] = tenantId.value.toJavaUuid()
            it[TenantInvitationsTable.email] = email.value
            it[TenantInvitationsTable.role] = role
            it[TenantInvitationsTable.invitedBy] = invitedBy.value.toJavaUuid()
            it[TenantInvitationsTable.token] = token
            it[TenantInvitationsTable.status] = InvitationStatus.Pending
            it[TenantInvitationsTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
        }.value

        logger.info("Created invitation $invitationId for ${email.value} to tenant $tenantId with role $role")
        InvitationId(invitationId.toKotlinUuid())
    }

    /**
     * Find an invitation by ID within a specific tenant.
     * CRITICAL: Always filter by tenantId for security.
     */
    suspend fun findByIdAndTenant(
        id: InvitationId,
        tenantId: TenantId
    ): TenantInvitation? = dbQuery {
        TenantInvitationsTable
            .join(UsersTable, JoinType.INNER, TenantInvitationsTable.invitedBy, UsersTable.id)
            .selectAll()
            .where {
                (TenantInvitationsTable.id eq id.value.toJavaUuid()) and
                        (TenantInvitationsTable.tenantId eq tenantId.value.toJavaUuid())
            }
            .singleOrNull()
            ?.toTenantInvitation()
    }

    /**
     * Find an invitation by its token.
     * Used when accepting an invitation.
     */
    suspend fun findByToken(token: String): TenantInvitation? = dbQuery {
        TenantInvitationsTable
            .join(UsersTable, JoinType.INNER, TenantInvitationsTable.invitedBy, UsersTable.id)
            .selectAll()
            .where { TenantInvitationsTable.token eq token }
            .singleOrNull()
            ?.toTenantInvitation()
    }

    /**
     * Find pending invitation by email.
     * Used to auto-join users when they register with an invited email.
     */
    suspend fun findPendingByEmail(email: Email): TenantInvitation? = dbQuery {
        TenantInvitationsTable
            .join(UsersTable, JoinType.INNER, TenantInvitationsTable.invitedBy, UsersTable.id)
            .selectAll()
            .where {
                (TenantInvitationsTable.email eq email.value) and
                        (TenantInvitationsTable.status eq InvitationStatus.Pending)
            }
            .singleOrNull()
            ?.toTenantInvitation()
    }

    /**
     * List all invitations for a tenant with optional status filter.
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        status: InvitationStatus? = null
    ): List<TenantInvitation> = dbQuery {
        val baseCondition = TenantInvitationsTable.tenantId eq tenantId.value.toJavaUuid()
        val condition = if (status != null) {
            baseCondition and (TenantInvitationsTable.status eq status)
        } else {
            baseCondition
        }

        TenantInvitationsTable
            .join(UsersTable, JoinType.INNER, TenantInvitationsTable.invitedBy, UsersTable.id)
            .selectAll()
            .where { condition }
            .orderBy(TenantInvitationsTable.createdAt)
            .map { it.toTenantInvitation() }
    }

    /**
     * Mark an invitation as accepted.
     */
    suspend fun markAccepted(
        id: InvitationId,
        acceptedBy: UserId,
        acceptedAt: Instant
    ): Unit = dbQuery {
        val updated = TenantInvitationsTable.update({
            TenantInvitationsTable.id eq id.value.toJavaUuid()
        }) {
            it[status] = InvitationStatus.Accepted
            it[TenantInvitationsTable.acceptedBy] = acceptedBy.value.toJavaUuid()
            it[TenantInvitationsTable.acceptedAt] = acceptedAt.toLocalDateTime(TimeZone.UTC)
        }

        if (updated == 0) {
            throw IllegalArgumentException("Invitation not found: $id")
        }

        logger.info("Invitation $id accepted by user $acceptedBy")
    }

    /**
     * Cancel an invitation.
     * CRITICAL: Always verify tenantId for security.
     */
    suspend fun cancel(id: InvitationId, tenantId: TenantId): Unit = dbQuery {
        val updated = TenantInvitationsTable.update({
            (TenantInvitationsTable.id eq id.value.toJavaUuid()) and
                    (TenantInvitationsTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[status] = InvitationStatus.Cancelled
        }

        if (updated == 0) {
            throw IllegalArgumentException("Invitation not found: $id")
        }

        logger.info("Cancelled invitation $id for tenant $tenantId")
    }

    /**
     * Mark expired invitations.
     * Should be called periodically by a scheduled job.
     */
    suspend fun markExpired(): Int = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val updated = TenantInvitationsTable.update({
            (TenantInvitationsTable.status eq InvitationStatus.Pending) and
                    (TenantInvitationsTable.expiresAt less now)
        }) {
            it[status] = InvitationStatus.Expired
        }

        if (updated > 0) {
            logger.info("Marked $updated invitations as expired")
        }

        updated
    }

    /**
     * Generate cryptographically secure token (256 bits).
     */
    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Extension function to map ResultRow to TenantInvitation.
     */
    private fun org.jetbrains.exposed.v1.core.ResultRow.toTenantInvitation(): TenantInvitation {
        val inviterFirstName = this[UsersTable.firstName]
        val inviterLastName = this[UsersTable.lastName]
        val inviterEmail = this[UsersTable.email]
        val inviterName = listOfNotNull(inviterFirstName, inviterLastName)
            .joinToString(" ")
            .ifEmpty { inviterEmail }

        return TenantInvitation(
            id = InvitationId(this[TenantInvitationsTable.id].value.toKotlinUuid()),
            tenantId = TenantId(this[TenantInvitationsTable.tenantId].value.toKotlinUuid()),
            email = Email(this[TenantInvitationsTable.email]),
            role = this[TenantInvitationsTable.role],
            invitedByName = inviterName,
            status = this[TenantInvitationsTable.status],
            expiresAt = this[TenantInvitationsTable.expiresAt],
            createdAt = this[TenantInvitationsTable.createdAt]
        )
    }
}
