package ai.thepredict.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

internal object ContactNotesTable : IntIdTable("contacts_notes") {
    val text = text("text")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    val contact = reference("contact", ContactsTable, onDelete = ReferenceOption.CASCADE)
    val workspace = reference("workspace", WorkspacesTable, onDelete = ReferenceOption.SET_NULL)
}

class ContactNoteEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ContactEntity>(ContactsTable)

    var text by ContactNotesTable.text

    val createdAt by ContactNotesTable.createdAt

    val contact by ContactEntity referencedOn ContactNotesTable.contact
    val workspace by WorkspaceEntity referencedOn ContactNotesTable.workspace
}