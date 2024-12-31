package ai.thepredict.database.tables

import ai.thepredict.domain.Contact
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

internal object ContactsTable : IntIdTable("contacts") {
    val name = varchar("name", 128)
    val phoneNumber = varchar("phone_number", 128).nullable()
    val email = varchar("email", 128).nullable()
    val taxNumber = varchar("tax_number", 128).nullable()
    val companyName = varchar("company_name", 128).nullable()
    val webUrl = varchar("web_url", 128).nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val state = varchar("state", 128).default(Contact.State.default.key)

    val workspace = reference("workspace", WorkspacesTable, onDelete = ReferenceOption.SET_NULL)
}

class ContactEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ContactEntity>(ContactsTable)

    var name by ContactsTable.name
    var phoneNumber by ContactsTable.phoneNumber
    var email by ContactsTable.email
    var taxNumber by ContactsTable.taxNumber
    var companyName by ContactsTable.companyName
    var webUrl by ContactsTable.webUrl

    val createdAt by ContactsTable.createdAt
    var state by ContactsTable.state

    val workspace by WorkspaceEntity referencedOn ContactsTable.workspace
}
