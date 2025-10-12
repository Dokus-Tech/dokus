package ai.dokus.foundation.database.tables

import ai.dokus.auth.domain.model.UserRole
import ai.dokus.foundation.ktor.db.dbEnumeration
import org.jetbrains.exposed.sql.Table

object UserRolesTable : Table("user_roles") {
    val userId = reference("user_id", UsersTable)
    val role = dbEnumeration<UserRole>("role")

    override val primaryKey = PrimaryKey(userId, role)
}