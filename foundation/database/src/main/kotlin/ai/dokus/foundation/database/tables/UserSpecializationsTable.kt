package ai.dokus.foundation.ktor.db.tables

import org.jetbrains.exposed.sql.Table

object UserSpecializationsTable : Table("user_specializations") {
    val userId = reference("user_id", UsersTable)
    val specialization = varchar("specialization", 100)

    override val primaryKey = PrimaryKey(userId, specialization)
}