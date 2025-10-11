package be.police.pulse.database.tables

import be.police.pulse.domain.model.UserRole
import be.police.pulse.foundation.ktor.db.dbEnumeration
import org.jetbrains.exposed.sql.Table

object UserRolesTable : Table("user_roles") {
    val userId = reference("user_id", UsersTable)
    val role = dbEnumeration<UserRole>("role")

    override val primaryKey = PrimaryKey(userId, role)
}