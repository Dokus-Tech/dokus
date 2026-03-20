package tech.dokus.database.repository.contacts

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.toContactNoteDto
import tech.dokus.database.tables.contacts.ContactNotesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import tech.dokus.foundation.backend.utils.runSuspendCatching

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
    ): Result<ContactNoteDto> = runSuspendCatching {
        dbQuery {
            // Verify contact exists and belongs to tenant
            val contactExists = ContactsTable.selectAll().where {
                (ContactsTable.id eq UUID.fromString(contactId.toString())) and
                    (ContactsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!contactExists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            val noteId = ContactNotesTable.insertAndGetId {
                it[ContactNotesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[ContactNotesTable.contactId] = UUID.fromString(contactId.toString())
                it[ContactNotesTable.content] = content
                it[ContactNotesTable.authorId] = authorId?.let { id -> UUID.fromString(id.toString()) }
                it[ContactNotesTable.authorName] = authorName
            }

            // Fetch and return the created note
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq noteId.value) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                row.toContactNoteDto()
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
    ): Result<ContactNoteDto?> = runSuspendCatching {
        dbQuery {
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq UUID.fromString(noteId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                row.toContactNoteDto()
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
    ): Result<PaginatedResponse<ContactNoteDto>> = runSuspendCatching {
        dbQuery {
            val query = ContactNotesTable.selectAll().where {
                (ContactNotesTable.contactId eq UUID.fromString(contactId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }

            val total = query.count()

            // Order by most recent first
            val items = query.orderBy(ContactNotesTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { it.toContactNoteDto() }
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
    ): Result<Long> = runSuspendCatching {
        dbQuery {
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.contactId eq UUID.fromString(contactId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
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
    ): Result<ContactNoteDto> = runSuspendCatching {
        dbQuery {
            // Verify note exists and belongs to tenant
            val exists = ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq UUID.fromString(noteId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Note not found or access denied")
            }

            ContactNotesTable.update({
                (ContactNotesTable.id eq UUID.fromString(noteId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[ContactNotesTable.content] = content
            }

            // Fetch and return the updated note
            ContactNotesTable.selectAll().where {
                (ContactNotesTable.id eq UUID.fromString(noteId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                row.toContactNoteDto()
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
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            val deletedRows = ContactNotesTable.deleteWhere {
                (ContactNotesTable.id eq UUID.fromString(noteId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
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
    ): Result<Int> = runSuspendCatching {
        dbQuery {
            ContactNotesTable.deleteWhere {
                (ContactNotesTable.contactId eq UUID.fromString(contactId.toString())) and
                    (ContactNotesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
        }
    }

}
