package ai.thepredict.database.tables

import ai.thepredict.database.Database
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.UUID

internal object UsersTable : UUIDTable("users") {
    val name = varchar("name", 128)
    val email = varchar("email", 128)
    val passwordHash = text("password_hash")
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val workspaces = reference("workspaces", WorkspacesTable)
}

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UsersTable)

    val name by UsersTable.name
    val email by UsersTable.email
    val passwordHash by UsersTable.passwordHash
    val createdAt by UsersTable.createdAt

    val workspaces by WorkspaceEntity referrersOn UsersTable.workspaces
}

fun UserEntity.Companion.getAll(): List<UserEntity> {
    return Database.transaction { UserEntity.all().toList() }
}

fun UserEntity.Companion.getById(userId: UUID): UserEntity? {
    return Database.transaction { runCatching { get(userId) }.getOrNull() }
}