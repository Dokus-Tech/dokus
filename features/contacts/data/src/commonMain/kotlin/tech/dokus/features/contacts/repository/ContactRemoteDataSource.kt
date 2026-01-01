package tech.dokus.features.contacts.repository

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.ContactStats
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactPeppolRequest
import tech.dokus.domain.model.contact.UpdateContactRequest

/**
 * Remote data source for Contact API operations.
 * Enables testability through dependency injection of test doubles.
 */
interface ContactRemoteDataSource {
    // CRUD Operations
    suspend fun listContacts(
        search: String? = null,
        isActive: Boolean? = null,
        peppolEnabled: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>

    suspend fun listCustomers(
        isActive: Boolean = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>

    suspend fun listVendors(
        isActive: Boolean = true,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactDto>>

    suspend fun getContact(contactId: ContactId): Result<ContactDto>

    suspend fun createContact(request: CreateContactRequest): Result<ContactDto>

    suspend fun updateContact(
        contactId: ContactId,
        request: UpdateContactRequest
    ): Result<ContactDto>

    suspend fun deleteContact(contactId: ContactId): Result<Unit>

    // Peppol Operations
    suspend fun updateContactPeppol(
        contactId: ContactId,
        request: UpdateContactPeppolRequest
    ): Result<ContactDto>

    // Activity Operations
    suspend fun getContactActivity(contactId: ContactId): Result<ContactActivitySummary>

    // Statistics
    suspend fun getContactStats(): Result<ContactStats>

    // Merge Operations
    suspend fun mergeContacts(
        sourceContactId: ContactId,
        targetContactId: ContactId
    ): Result<ContactMergeResult>

    // Notes Operations
    suspend fun listNotes(
        contactId: ContactId,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ContactNoteDto>>

    suspend fun createNote(
        contactId: ContactId,
        request: CreateContactNoteRequest
    ): Result<ContactNoteDto>

    suspend fun updateNote(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ): Result<ContactNoteDto>

    suspend fun deleteNote(
        contactId: ContactId,
        noteId: ContactNoteId
    ): Result<Unit>
}
