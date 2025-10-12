package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.entity.LoginResult
import ai.dokus.foundation.ktor.db.dbEnumeration
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UserLoginAttemptsTable : UUIDTable("user_login_attempts") {
    val matricule = varchar("matricule", 255).nullable().index()
    val email = varchar("email", 255).nullable().index()
    val userId = reference("user_id", UsersTable).nullable()
    val ipAddress = varchar("ip_address", 45)
    val userAgent = text("user_agent").nullable()
    val result = dbEnumeration<LoginResult>("result")
    val failureReason = text("failure_reason").nullable()
    val attemptedAt = timestamp("attempted_at")
}