package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Durable welcome email queue.
 * Ensures welcome email is sent once per user with retry/backoff support.
 */
object WelcomeEmailJobsTable : UUIDTable("welcome_email_jobs") {
    enum class JobStatus {
        Pending,
        Processing,
        Retry,
        Sent
    }

    val userId = uuid("user_id").references(
        UsersTable.id,
        onDelete = ReferenceOption.CASCADE
    ).uniqueIndex()

    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    val status = enumerationByName<JobStatus>("status", 20).index()
    val scheduledAt = datetime("scheduled_at")
    val nextAttemptAt = datetime("next_attempt_at")
    val attemptCount = integer("attempt_count").default(0)
    val lastError = text("last_error").nullable()
    val sentAt = datetime("sent_at").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, status, nextAttemptAt)
    }
}
