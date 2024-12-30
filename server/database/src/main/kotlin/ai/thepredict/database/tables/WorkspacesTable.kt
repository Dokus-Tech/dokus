package ai.thepredict.database.tables

import ai.thepredict.database.Database
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

internal object WorkspacesTable : IntIdTable("workspaces") {
    val name = varchar("name", 128)
    val legalName = varchar("legal_name", 128).nullable()
    val vatNumber = varchar("vat_number", 128).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val owner = reference("owner", UsersTable)

    init {
        foreignKey(owner, target = UsersTable.primaryKey)
    }
}

class WorkspaceEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkspaceEntity>(WorkspacesTable)

    private val _workspaceId by WorkspacesTable.id
    val workspaceId = _workspaceId.value

    val name by WorkspacesTable.name
    val legalName by WorkspacesTable.legalName
    val vatNumber by WorkspacesTable.vatNumber

    val owner by UserEntity referrersOn WorkspacesTable
}

suspend fun WorkspaceEntity.Companion.getById(workspaceId: Int): WorkspaceEntity? {
    return Database.transaction { runCatching { get(workspaceId) }.getOrNull() }
}
//
//suspend fun WorkspaceEntity.Companion.getByOwner(ownerId: Int): WorkspaceEntity? {
//    return Database.transaction { WorkspaceEntity.find { WorkspacesTable.owner eq ownerId } }
//        .getOrNull()
//}
//}