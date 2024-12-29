package ai.thepredict.database.tables

import ai.thepredict.database.Database
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.UUID

internal object WorkspacesTable : UUIDTable("workspaces") {
    val name = varchar("name", 128)
    val legalName = varchar("legal_name", 128).nullable()
    val vatNumber = varchar("vat_number", 128).nullable()
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
}

class WorkspaceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<WorkspaceEntity>(WorkspacesTable)

    private val _workspaceId by WorkspacesTable.id
    val workspaceId = _workspaceId.value

    val name by WorkspacesTable.name
    val legalName by WorkspacesTable.legalName
    val vatNumber by WorkspacesTable.vatNumber

}

fun WorkspaceEntity.Companion.getById(workspaceId: UUID): WorkspaceEntity? {
    return Database.transaction { runCatching { get(workspaceId) }.getOrNull() }
}