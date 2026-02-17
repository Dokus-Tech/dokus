package tech.dokus.database.repository.contacts
import kotlin.uuid.Uuid

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.backend.database.dbQuery

/**
 * Repository for managing contact notes
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return notes from different tenants
 * 3. All operations must be tenant-isolated
 */
class ContactNoteRepository {

    /**
     * Create a new note for a contact
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createNote(
        tenantId: TenantId,
        contactId: ContactId,
        content: String,
        authorId: UserId? = null,
        authorName: String? = null
    ): Result<ContactNoteDto> = runCatching {
        dbQuery {
            // Verify contact exists and belongs to tenant
            val contactExists = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactId.value) and
                    (ContactsTable.tenantId eq tenantId.value)
            }.count() > 0

            if (!contactExists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            val noteId = ContactNotesTable.insertAndGetId {
                it[ContactNotesTable.tenantId] = tenantId.value
                it[ContactNotesTable.contactId] = contactId.value
                it[ContactNotesTable.content] = content
                it[ContactNotesTable.authorId] = authorId?.let { id -> id.value }
                it[ContactNotesTable.authorName] = authorName
            }

            // Fetch and return the created note
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }.single().let { row ->
                mapRowToContactNoteDto(row)
            }
        }
    }

    /**
     * Get a single note by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getNote(
        noteId: ContactNoteId,
        tenantId: TenantId
    ): Result<ContactNoteDto?> = runCatching {
        dbQuery {
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }.singleOrNull()?.let { row ->
                mapRowToContactNoteDto(row)
            }
        }
    }

    /**
     * List notes for a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listNotes(
        contactId: ContactId,
        tenantId: TenantId,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactNoteDto>> = runCatching {
        dbQuery {
            val query = ContactNotesTable.selectAll().where {
                (ContactNotesTable.contactId eq contactId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }

            val total = query.count()

            // Order by most recent first
            val items = query.orderBy(ContactNotesTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { row -> mapRowToContactNoteDto(row) }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Count notes for a contact
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun countNotes(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Long> = runCatching {
        dbQuery {
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.contactId eq contactId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }.count()
        }
    }

    /**
     * Update a note
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateNote(
        noteId: ContactNoteId,
        tenantId: TenantId,
        content: String
    ): Result<ContactNoteDto> = runCatching {
        dbQuery {
            // Verify note exists and belongs to tenant
            val exists = ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Note not found or access denied")
            }

            ContactNotesTable.update({
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }) {
                it[ContactNotesTable.content] = content
            }

            // Fetch and return the updated note
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }.single().let { row ->
                mapRowToContactNoteDto(row)
            }
        }
    }

    /**
     * Delete a note
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteNote(
        noteId: ContactNoteId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = ContactNotesTable.deleteWhere {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }
            deletedRows > 0
        }
    }

    /**
     * Delete all notes for a contact
     * CRITICAL: MUST filter by tenant_id
     * Note: This is typically called by cascade delete, but provided for explicit use
     */
    suspend fun deleteAllNotesForContact(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Int> = runCatching {
        dbQuery {
            ContactNotesTable.deleteWhere {
                (ContactNotesTable.contactId eq contactId.value) and
                    (ContactNotesTable.tenantId eq tenantId.value)
            }
        }
    }

    /**
     * Map a database row to ContactNoteDto
     */
    private fun mapRowToContactNoteDto(row: ResultRow): ContactNoteDto {
        return ContactNoteDto(
            id = ContactNoteId(row[ContactNotesTable.id].value),
            contactId = ContactId(row[ContactNotesTable.contactId]),
            tenantId = TenantId(row[ContactNotesTable.tenantId]),
            content = row[ContactNotesTable.content],
            authorId = row[ContactNotesTable.authorId]?.let { UserId(it.toString()) },
            authorName = row[ContactNotesTable.authorName],
            createdAt = row[ContactNotesTable.createdAt],
            updatedAt = row[ContactNotesTable.updatedAt]
        )
    }
}
