package ai.thepredict.database.tables

import ai.thepredict.database.Database
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

internal object WorkspacesTable : IntIdTable("workspaces") {
    val name = varchar("name", 128)
    val legalName = varchar("legal_name", 128).nullable()
    val taxNumber = varchar("tax_number", 128).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val owner = reference("owner", UsersTable, onDelete = ReferenceOption.CASCADE)
}

class WorkspaceEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<WorkspaceEntity>(WorkspacesTable)

    private val _workspaceId by WorkspacesTable.id
    val workspaceId get() = _workspaceId.value

    var name by WorkspacesTable.name
    var legalName by WorkspacesTable.legalName
    var taxNumber by WorkspacesTable.taxNumber
    var createdAt by WorkspacesTable.createdAt

    val owner by UserEntity referencedOn WorkspacesTable.owner
    val contacts by ContactEntity referencedOn ContactsTable.workspace
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