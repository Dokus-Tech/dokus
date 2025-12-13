package ai.dokus.contacts.backend.routes

import ai.dokus.contacts.backend.service.ContactNoteService
import ai.dokus.contacts.backend.service.ContactService
import ai.dokus.foundation.domain.enums.ContactType
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Contact API Routes
 * Base path: /api/v1/contacts
 *
 * Provides endpoints for:
 * - CRUD operations on contacts (customers AND vendors)
 * - Contact notes management
 * - Peppol-specific operations
 * - Contact statistics
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.contactRoutes() {
    val contactService by inject<ContactService>()
    val contactNoteService by inject<ContactNoteService>()

    route("/api/v1/contacts") {
        authenticateJwt {

            // ================================================================
            // CRUD OPERATIONS
            // ================================================================

            /**
             * POST /api/v1/contacts
             * Create a new contact.
             */
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateContactRequest>()

                val contact = contactService.createContact(tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to create contact: ${it.message}") }

                // Create initial note if provided
                if (!request.initialNote.isNullOrBlank()) {
                    val principal = dokusPrincipal
                    contactNoteService.createNote(
                        tenantId = tenantId,
                        contactId = contact.id,
                        content = request.initialNote,
                        authorId = principal.userId,
                        authorName = principal.email
                    )
                }

                call.respond(HttpStatusCode.Created, contact)
            }

            /**
             * GET /api/v1/contacts/{contactId}
             * Get a contact by ID.
             */
            get("/{contactId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val contact = contactService.getContact(contactId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch contact: ${it.message}") }
                    ?: throw DokusException.NotFound("Contact not found")

                call.respond(HttpStatusCode.OK, contact)
            }

            /**
             * GET /api/v1/contacts
             * List contacts with optional filters.
             *
             * Query parameters:
             * - search: Search by name, email, or VAT number
             * - type: Filter by ContactType (CUSTOMER, VENDOR, BOTH)
             * - activeOnly: Filter by active status (true/false)
             * - peppolEnabled: Filter by Peppol enabled status (true/false)
             * - limit: Max results (default 50, max 200)
             * - offset: Pagination offset (default 0)
             */
            get {
                val tenantId = dokusPrincipal.requireTenantId()
                val searchQuery = call.parameters["search"]
                val contactType = call.parameters["type"]?.let {
                    try { ContactType.valueOf(it.uppercase()) } catch (e: Exception) { null }
                }
                val isActive = call.parameters["activeOnly"]?.toBooleanStrictOrNull()
                val peppolEnabled = call.parameters["peppolEnabled"]?.toBooleanStrictOrNull()
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                if (limit < 1 || limit > 200) {
                    throw DokusException.BadRequest("Limit must be between 1 and 200")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val contacts = contactService.listContacts(
                    tenantId = tenantId,
                    contactType = contactType,
                    isActive = isActive,
                    peppolEnabled = peppolEnabled,
                    searchQuery = searchQuery,
                    limit = limit,
                    offset = offset
                ).getOrElse { throw DokusException.InternalError("Failed to list contacts: ${it.message}") }

                call.respond(HttpStatusCode.OK, contacts)
            }

            /**
             * PUT /api/v1/contacts/{contactId}
             * Update a contact.
             */
            put("/{contactId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val request = call.receive<UpdateContactRequest>()

                val contact = contactService.updateContact(contactId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to update contact: ${it.message}") }

                call.respond(HttpStatusCode.OK, contact)
            }

            /**
             * DELETE /api/v1/contacts/{contactId}
             * Delete a contact.
             */
            delete("/{contactId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                contactService.deleteContact(contactId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete contact: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }

            // ================================================================
            // STATUS MANAGEMENT
            // ================================================================

            /**
             * POST /api/v1/contacts/{contactId}/deactivate
             * Deactivate a contact (soft delete).
             */
            post("/{contactId}/deactivate") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val success = contactService.deactivateContact(contactId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to deactivate contact: ${it.message}") }

                if (!success) {
                    throw DokusException.NotFound("Contact not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Contact deactivated"))
            }

            /**
             * POST /api/v1/contacts/{contactId}/reactivate
             * Reactivate a deactivated contact.
             */
            post("/{contactId}/reactivate") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val success = contactService.reactivateContact(contactId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to reactivate contact: ${it.message}") }

                if (!success) {
                    throw DokusException.NotFound("Contact not found")
                }

                call.respond(HttpStatusCode.OK, mapOf("message" to "Contact reactivated"))
            }

            // ================================================================
            // PEPPOL OPERATIONS
            // ================================================================

            /**
             * PATCH /api/v1/contacts/{contactId}/peppol
             * Update a contact's Peppol settings.
             */
            patch("/{contactId}/peppol") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val request = call.receive<UpdateContactPeppolRequest>()

                val contact = contactService.updateContactPeppol(
                    contactId = contactId,
                    tenantId = tenantId,
                    peppolId = request.peppolId,
                    peppolEnabled = request.peppolEnabled
                ).getOrElse { throw DokusException.InternalError("Failed to update contact Peppol settings: ${it.message}") }

                call.respond(HttpStatusCode.OK, contact)
            }

            /**
             * GET /api/v1/contacts/peppol-enabled
             * List all Peppol-enabled contacts.
             */
            get("/peppol-enabled") {
                val tenantId = dokusPrincipal.requireTenantId()

                val contacts = contactService.listPeppolEnabledContacts(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to list Peppol-enabled contacts: ${it.message}") }

                call.respond(HttpStatusCode.OK, contacts)
            }

            // ================================================================
            // CONTACT TYPE FILTERS
            // ================================================================

            /**
             * GET /api/v1/contacts/customers
             * List customers only (ContactType.Customer or ContactType.Both).
             */
            get("/customers") {
                val tenantId = dokusPrincipal.requireTenantId()
                val isActive = call.parameters["activeOnly"]?.toBooleanStrictOrNull() ?: true
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                val contacts = contactService.listCustomers(tenantId, isActive, limit, offset)
                    .getOrElse { throw DokusException.InternalError("Failed to list customers: ${it.message}") }

                call.respond(HttpStatusCode.OK, contacts)
            }

            /**
             * GET /api/v1/contacts/vendors
             * List vendors only (ContactType.Vendor or ContactType.Both).
             */
            get("/vendors") {
                val tenantId = dokusPrincipal.requireTenantId()
                val isActive = call.parameters["activeOnly"]?.toBooleanStrictOrNull() ?: true
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                val contacts = contactService.listVendors(tenantId, isActive, limit, offset)
                    .getOrElse { throw DokusException.InternalError("Failed to list vendors: ${it.message}") }

                call.respond(HttpStatusCode.OK, contacts)
            }

            // ================================================================
            // STATISTICS
            // ================================================================

            /**
             * GET /api/v1/contacts/stats
             * Get contact statistics for dashboard.
             */
            get("/stats") {
                val tenantId = dokusPrincipal.requireTenantId()

                val stats = contactService.getContactStats(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to get contact stats: ${it.message}") }

                call.respond(HttpStatusCode.OK, stats)
            }

            // ================================================================
            // NOTES OPERATIONS
            // ================================================================

            /**
             * GET /api/v1/contacts/{contactId}/notes
             * List notes for a contact.
             */
            get("/{contactId}/notes") {
                val tenantId = dokusPrincipal.requireTenantId()
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.parameters["offset"]?.toIntOrNull() ?: 0

                val notes = contactNoteService.listNotes(contactId, tenantId, limit, offset)
                    .getOrElse { throw DokusException.InternalError("Failed to list notes: ${it.message}") }

                call.respond(HttpStatusCode.OK, notes)
            }

            /**
             * POST /api/v1/contacts/{contactId}/notes
             * Add a note to a contact.
             */
            post("/{contactId}/notes") {
                val tenantId = dokusPrincipal.requireTenantId()
                val principal = dokusPrincipal
                val contactId = call.parameters["contactId"]?.let { ContactId.parse(it) }
                    ?: throw DokusException.BadRequest("Contact ID is required")

                val request = call.receive<CreateContactNoteRequest>()

                val note = contactNoteService.createNote(
                    tenantId = tenantId,
                    contactId = contactId,
                    content = request.content,
                    authorId = principal.userId,
                    authorName = principal.email
                ).getOrElse { throw DokusException.InternalError("Failed to create note: ${it.message}") }

                call.respond(HttpStatusCode.Created, note)
            }

            /**
             * PUT /api/v1/contacts/{contactId}/notes/{noteId}
             * Update a note.
             */
            put("/{contactId}/notes/{noteId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val noteId = call.parameters["noteId"]?.let { ContactNoteId.parse(it) }
                    ?: throw DokusException.BadRequest("Note ID is required")

                val request = call.receive<UpdateContactNoteRequest>()

                val note = contactNoteService.updateNote(noteId, tenantId, request.content)
                    .getOrElse { throw DokusException.InternalError("Failed to update note: ${it.message}") }

                call.respond(HttpStatusCode.OK, note)
            }

            /**
             * DELETE /api/v1/contacts/{contactId}/notes/{noteId}
             * Delete a note.
             */
            delete("/{contactId}/notes/{noteId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val noteId = call.parameters["noteId"]?.let { ContactNoteId.parse(it) }
                    ?: throw DokusException.BadRequest("Note ID is required")

                contactNoteService.deleteNote(noteId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete note: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
