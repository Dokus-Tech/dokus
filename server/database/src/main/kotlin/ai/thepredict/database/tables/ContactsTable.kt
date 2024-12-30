package ai.thepredict.database.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption

internal object ContactsTable : IntIdTable("contacts") {
    val name = varchar("name", 128)
    val phoneNumber = varchar("phone_number", 128).nullable()
    val email = varchar("email", 128).nullable()
    val taxNumber = varchar("tax_number", 128).nullable()
    val companyName = varchar("company_name", 128).nullable()
    val webUrl = varchar("web_url", 128).nullable()

    val workspace = reference("workspace", WorkspacesTable, onDelete = ReferenceOption.SET_NULL)
}

class ContactsEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ContactsEntity>(ContactsTable)

    val name by ContactsTable.name
    val phoneNumber by ContactsTable.phoneNumber
    val email by ContactsTable.email
    val taxNumber by ContactsTable.taxNumber
    val companyName by ContactsTable.companyName
    val webUrl by ContactsTable.webUrl

    val workspace by WorkspaceEntity referencedOn ContactsTable.workspace
}
