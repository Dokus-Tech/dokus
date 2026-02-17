package tech.dokus.database.tables.contacts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable

/**
 * Contact notes table - stores notes history for contacts.
 * Each note is timestamped and tracks the author for audit purposes.
 *
 * OWNER: contacts service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object ContactNotesTable : UuidTable("contact_notes") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Parent contact
    val contactId = uuid("contact_id").references(
        ContactsTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Note content
    val content = text("content")

    // Author tracking
    val authorId = uuid("author_id").references(
        UsersTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable()
    val authorName = varchar("author_name", 255).nullable() // Denormalized for display

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Index for listing notes by contact (ordered by creation time)
        index(false, contactId, createdAt)
        // Index for listing notes by tenant
        index(false, tenantId, createdAt)
    }
}
