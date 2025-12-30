package tech.dokus.backend.services.contacts

import ai.dokus.foundation.database.repository.contacts.ContactNoteRepository
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for contact notes business operations.
 *
 * Manages notes history for contacts, including author tracking.
 */
class ContactNoteService(
    private val contactNoteRepository: ContactNoteRepository
) {
    private val logger = loggerFor()

    /**
     * Create a new note for a contact.
     */
    suspend fun createNote(
        tenantId: TenantId,
        contactId: ContactId,
        content: String,
        authorId: UserId? = null,
        authorName: String? = null
    ): Result<ContactNoteDto> {
        logger.info("Creating note for contact: $contactId by author: $authorName")
        return contactNoteRepository.createNote(tenantId, contactId, content, authorId, authorName)
            .onSuccess { logger.info("Note created: ${it.id} for contact: $contactId") }
            .onFailure { logger.error("Failed to create note for contact: $contactId", it) }
    }

    /**
     * Get a note by ID.
     */
    suspend fun getNote(
        noteId: ContactNoteId,
        tenantId: TenantId
    ): Result<ContactNoteDto?> {
        logger.debug("Fetching note: {}", noteId)
        return contactNoteRepository.getNote(noteId, tenantId)
            .onFailure { logger.error("Failed to fetch note: $noteId", it) }
    }

    /**
     * List notes for a contact.
     */
    suspend fun listNotes(
        contactId: ContactId,
        tenantId: TenantId,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ContactNoteDto>> {
        logger.debug(
            "Listing notes for contact: {} (limit={}, offset={})",
            contactId,
            limit,
            offset
        )
        return contactNoteRepository.listNotes(contactId, tenantId, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} notes (total=${it.total})") }
            .onFailure { logger.error("Failed to list notes for contact: $contactId", it) }
    }

    /**
     * Count notes for a contact.
     */
    suspend fun countNotes(
        contactId: ContactId,
        tenantId: TenantId
    ): Result<Long> {
        return contactNoteRepository.countNotes(contactId, tenantId)
    }

    /**
     * Update a note.
     */
    suspend fun updateNote(
        noteId: ContactNoteId,
        tenantId: TenantId,
        content: String
    ): Result<ContactNoteDto> {
        logger.info("Updating note: $noteId")
        return contactNoteRepository.updateNote(noteId, tenantId, content)
            .onSuccess { logger.info("Note updated: $noteId") }
            .onFailure { logger.error("Failed to update note: $noteId", it) }
    }

    /**
     * Delete a note.
     */
    suspend fun deleteNote(
        noteId: ContactNoteId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting note: $noteId")
        return contactNoteRepository.deleteNote(noteId, tenantId)
            .onSuccess { logger.info("Note deleted: $noteId") }
            .onFailure { logger.error("Failed to delete note: $noteId", it) }
    }
}
