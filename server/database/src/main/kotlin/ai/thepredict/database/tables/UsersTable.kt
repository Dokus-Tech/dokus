package ai.thepredict.database.tables

import ai.thepredict.data.AuthCredentials
import ai.thepredict.data.userUUID
import ai.thepredict.database.Database
import ai.thepredict.domain.exceptions.PredictException
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal object UsersTable : UUIDTable("users") {
    val name = varchar("name", 128)
    val email = varchar("email", 128)
    val passwordHash = text("password_hash")

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UsersTable)

    private val _userId by UsersTable.id
    val userId get() = _userId.value

    var name by UsersTable.name
    var email by UsersTable.email
    var passwordHash by UsersTable.passwordHash

    val createdAt by UsersTable.createdAt

    val workspaces by WorkspaceEntity referrersOn WorkspacesTable.owner
}

suspend fun UserEntity.Companion.getAll(): List<UserEntity> {
    return Database.transaction { UserEntity.all().toList() }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun UserEntity.Companion.getById(userId: Uuid): UserEntity? {
    return Database.transaction { runCatching { get(userId.toJavaUuid()) }.getOrNull() }
}

suspend fun UserEntity.Companion.getById(userId: UUID): UserEntity? {
    return Database.transaction { findById(userId) }
}

suspend fun UserEntity.Companion.findByEmail(email: String): UserEntity? {
    return Database.transaction { find { UsersTable.email eq email }.singleOrNull() }
}

@OptIn(ExperimentalUuidApi::class)
@Throws(PredictException.NotAuthenticated::class)
suspend fun UserEntity.Companion.authenticated(authCredentials: AuthCredentials): UserEntity {
    val user = UserEntity.getById(authCredentials.userUUID.toJavaUuid())
    if (user == null || user.passwordHash != authCredentials.passwordHash) throw PredictException.NotAuthenticated
    return user
}