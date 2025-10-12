package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.sql.Table

object UserPermissionsTable : Table("user_permissions") {
    val userId = reference("user_id", UsersTable)
    val permission = varchar("permission", 255)

    override val primaryKey = PrimaryKey(userId, permission)
}