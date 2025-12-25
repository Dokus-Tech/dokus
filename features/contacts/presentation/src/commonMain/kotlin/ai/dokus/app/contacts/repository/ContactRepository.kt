package ai.dokus.app.contacts.repository

import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactMergeResult
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.domain.model.ContactStats
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.domain.routes.Contacts
import ai.dokus.foundation.platform.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Repository for Contact API operations.
 * Uses Ktor HttpClient with type-safe routing to communicate with the contacts API.
 *
 * Error Handling:
 * - All methods return Result<T> for safe error propagation
 * - Uses runCatching to wrap API calls
 */
class ContactRepository(
    private val httpClient: HttpClient
) : ContactRepositoryApi {
    private val logger = Logger.forClass<ContactRepository>()

    // ============================================================================
    // CRUD OPERATIONS
    // ============================================================================

    /**
     * List contacts with optional filters.
     *
     * @param search Search query for name, email, VAT number
     * @param isActive Filter by active/inactive status
     * @param peppolEnabled Filter by Peppol enabled status
     * @param limit Maximum number of results (1-200)
     * @param offset Pagination offset
     * @return Result containing list of contacts
     */
    override suspend fun listContacts(
        search: String?,
        isActive: Boolean?,
        peppolEnabled: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        logger.d { "Listing contacts: search=$search, active=$isActive, limit=$limit, offset=$offset" }
        return runCatching {
            val response = httpClient.get(
                Contacts(
                    search = search,
                    active = isActive,
                    peppolEnabled = peppolEnabled,
                    limit = limit,
                    offset = offset
                )
            ).body<PaginatedResponse<ContactDto>>()
            response.items
        }.onSuccess { contacts ->
            logger.i { "Listed ${contacts.size} contacts" }
        }.onFailure { error ->
            logger.e(error) { "Failed to list contacts" }
        }
    }

    /**
     * List customer contacts only (contacts with outgoing invoices).
     *
     * @param isActive Filter by active/inactive status
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Result containing list of customer contacts
     */
    override suspend fun listCustomers(
        isActive: Boolean,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        logger.d { "Listing customers: active=$isActive, limit=$limit, offset=$offset" }
        return runCatching {
            val response = httpClient.get(
                Contacts.Customers(
                    active = isActive,
                    limit = limit,
                    offset = offset
                )
            ).body<PaginatedResponse<ContactDto>>()
            response.items
        }.onSuccess { contacts ->
            logger.i { "Listed ${contacts.size} customers" }
        }.onFailure { error ->
            logger.e(error) { "Failed to list customers" }
        }
    }

    /**
     * List vendor contacts only (contacts with incoming bills).
     *
     * @param isActive Filter by active/inactive status
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Result containing list of vendor contacts
     */
    override suspend fun listVendors(
        isActive: Boolean,
        limit: Int,
        offset: Int
    ): Result<List<ContactDto>> {
        logger.d { "Listing vendors: active=$isActive, limit=$limit, offset=$offset" }
        return runCatching {
            val response = httpClient.get(
                Contacts.Vendors(
                    active = isActive,
                    limit = limit,
                    offset = offset
                )
            ).body<PaginatedResponse<ContactDto>>()
            response.items
        }.onSuccess { contacts ->
            logger.i { "Listed ${contacts.size} vendors" }
        }.onFailure { error ->
            logger.e(error) { "Failed to list vendors" }
        }
    }

    /**
     * Get a single contact by ID.
     *
     * @param contactId The contact ID
     * @return Result containing the contact or null if not found
     */
    override suspend fun getContact(contactId: ContactId): Result<ContactDto> {
        logger.d { "Getting contact: $contactId" }
        return runCatching {
            httpClient.get(
                Contacts.Id(id = contactId.toString())
            ).body<ContactDto>()
        }.onSuccess { contact ->
            logger.i { "Got contact: ${contact.name}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to get contact: $contactId" }
        }
    }

    /**
     * Create a new contact.
     *
     * @param request Contact creation data
     * @return Result containing the created contact
     */
    override suspend fun createContact(request: CreateContactRequest): Result<ContactDto> {
        logger.d { "Creating contact: ${request.name}" }
        return runCatching {
            httpClient.post(Contacts()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ContactDto>()
        }.onSuccess { contact ->
            logger.i { "Created contact: ${contact.id}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to create contact" }
        }
    }

    /**
     * Update an existing contact.
     *
     * @param contactId The contact ID
     * @param request Contact update data (only provided fields will be updated)
     * @return Result containing the updated contact
     */
    override suspend fun updateContact(
        contactId: ContactId,
        request: UpdateContactRequest
    ): Result<ContactDto> {
        logger.d { "Updating contact: $contactId" }
        return runCatching {
            httpClient.put(Contacts.Id(id = contactId.toString())) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ContactDto>()
        }.onSuccess { contact ->
            logger.i { "Updated contact: ${contact.id}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to update contact: $contactId" }
        }
    }

    /**
     * Delete a contact.
     *
     * @param contactId The contact ID
     * @return Result indicating success or failure
     */
    override suspend fun deleteContact(contactId: ContactId): Result<Unit> {
        logger.d { "Deleting contact: $contactId" }
        return runCatching {
            httpClient.delete(Contacts.Id(id = contactId.toString())).body<Unit>()
        }.onSuccess {
            logger.i { "Deleted contact: $contactId" }
        }.onFailure { error ->
            logger.e(error) { "Failed to delete contact: $contactId" }
        }
    }

    // ============================================================================
    // PEPPOL OPERATIONS
    // ============================================================================

    /**
     * Update a contact's Peppol settings.
     *
     * @param contactId The contact ID
     * @param request Peppol update data
     * @return Result containing the updated contact
     */
    override suspend fun updateContactPeppol(
        contactId: ContactId,
        request: UpdateContactPeppolRequest
    ): Result<ContactDto> {
        logger.d { "Updating contact Peppol: $contactId, enabled=${request.peppolEnabled}" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            httpClient.patch(Contacts.Id.Peppol(parent = contactIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ContactDto>()
        }.onSuccess { contact ->
            logger.i { "Updated contact Peppol settings: ${contact.id}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to update contact Peppol: $contactId" }
        }
    }

    // ============================================================================
    // ACTIVITY OPERATIONS
    // ============================================================================

    /**
     * Get activity summary for a contact.
     * Returns counts and totals for invoices, bills, and expenses.
     *
     * @param contactId The contact ID
     * @return Result containing the activity summary
     */
    override suspend fun getContactActivity(contactId: ContactId): Result<ContactActivitySummary> {
        logger.d { "Getting contact activity: $contactId" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            httpClient.get(Contacts.Id.Activity(parent = contactIdRoute)).body<ContactActivitySummary>()
        }.onSuccess { activity ->
            logger.i { "Got contact activity: invoices=${activity.invoiceCount}, bills=${activity.billCount}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to get contact activity: $contactId" }
        }
    }

    // ============================================================================
    // STATISTICS
    // ============================================================================

    /**
     * Get contact statistics for dashboard.
     *
     * @return Result containing contact statistics
     */
    override suspend fun getContactStats(): Result<ContactStats> {
        logger.d { "Getting contact stats" }
        return runCatching {
            httpClient.get(Contacts.Summary()).body<ContactStats>()
        }.onSuccess { stats ->
            logger.i { "Got contact stats: total=${stats.totalContacts}, active=${stats.activeContacts}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to get contact stats" }
        }
    }

    // ============================================================================
    // MERGE OPERATIONS
    // ============================================================================

    /**
     * Merge source contact into target contact.
     * All cashflow items (invoices, bills, expenses) and notes from the source
     * contact are reassigned to the target contact. The source contact is archived.
     *
     * @param sourceContactId The contact to merge from (will be archived)
     * @param targetContactId The contact to merge into
     * @return Result containing merge result with reassignment counts
     */
    override suspend fun mergeContacts(
        sourceContactId: ContactId,
        targetContactId: ContactId
    ): Result<ContactMergeResult> {
        logger.d { "Merging contact $sourceContactId into $targetContactId" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = sourceContactId.toString())
            httpClient.post(
                Contacts.Id.MergeInto(
                    parent = contactIdRoute,
                    targetId = targetContactId.toString()
                )
            ).body<ContactMergeResult>()
        }.onSuccess { result ->
            logger.i {
                "Merged contacts: invoices=${result.invoicesReassigned}, " +
                    "bills=${result.billsReassigned}, expenses=${result.expensesReassigned}"
            }
        }.onFailure { error ->
            logger.e(error) { "Failed to merge contacts" }
        }
    }

    // ============================================================================
    // NOTES OPERATIONS
    // ============================================================================

    /**
     * List notes for a contact.
     *
     * @param contactId The contact ID
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Result containing list of notes
     */
    override suspend fun listNotes(
        contactId: ContactId,
        limit: Int,
        offset: Int
    ): Result<List<ContactNoteDto>> {
        logger.d { "Listing notes for contact: $contactId, limit=$limit, offset=$offset" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            val response = httpClient.get(
                Contacts.Id.Notes(
                    parent = contactIdRoute,
                    limit = limit,
                    offset = offset
                )
            ).body<PaginatedResponse<ContactNoteDto>>()
            response.items
        }.onSuccess { notes ->
            logger.i { "Listed ${notes.size} notes for contact: $contactId" }
        }.onFailure { error ->
            logger.e(error) { "Failed to list notes for contact: $contactId" }
        }
    }

    /**
     * Create a note for a contact.
     *
     * @param contactId The contact ID
     * @param request Note creation data
     * @return Result containing the created note
     */
    override suspend fun createNote(
        contactId: ContactId,
        request: CreateContactNoteRequest
    ): Result<ContactNoteDto> {
        logger.d { "Creating note for contact: $contactId" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            httpClient.post(Contacts.Id.Notes(parent = contactIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ContactNoteDto>()
        }.onSuccess { note ->
            logger.i { "Created note: ${note.id} for contact: $contactId" }
        }.onFailure { error ->
            logger.e(error) { "Failed to create note for contact: $contactId" }
        }
    }

    /**
     * Update a note.
     *
     * @param contactId The contact ID
     * @param noteId The note ID
     * @param request Note update data
     * @return Result containing the updated note
     */
    override suspend fun updateNote(
        contactId: ContactId,
        noteId: ContactNoteId,
        request: UpdateContactNoteRequest
    ): Result<ContactNoteDto> {
        logger.d { "Updating note: $noteId for contact: $contactId" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            val notesRoute = Contacts.Id.Notes(parent = contactIdRoute)
            httpClient.put(Contacts.Id.Notes.ById(parent = notesRoute, noteId = noteId.toString())) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<ContactNoteDto>()
        }.onSuccess { note ->
            logger.i { "Updated note: ${note.id}" }
        }.onFailure { error ->
            logger.e(error) { "Failed to update note: $noteId" }
        }
    }

    /**
     * Delete a note.
     *
     * @param contactId The contact ID
     * @param noteId The note ID
     * @return Result indicating success or failure
     */
    override suspend fun deleteNote(
        contactId: ContactId,
        noteId: ContactNoteId
    ): Result<Unit> {
        logger.d { "Deleting note: $noteId for contact: $contactId" }
        return runCatching {
            val contactIdRoute = Contacts.Id(id = contactId.toString())
            val notesRoute = Contacts.Id.Notes(parent = contactIdRoute)
            httpClient.delete(
                Contacts.Id.Notes.ById(parent = notesRoute, noteId = noteId.toString())
            ).body<Unit>()
        }.onSuccess {
            logger.i { "Deleted note: $noteId" }
        }.onFailure { error ->
            logger.e(error) { "Failed to delete note: $noteId" }
        }
    }
}
