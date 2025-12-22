package ai.dokus.contacts.backend.routes

import ai.dokus.contacts.backend.service.ContactNoteService
import ai.dokus.contacts.backend.service.ContactService
import ai.dokus.foundation.database.repository.cashflow.CashflowRepository
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.ContactNoteId
import ai.dokus.foundation.domain.model.CreateContactNoteRequest
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactPeppolRequest
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactNoteRequest
import ai.dokus.foundation.domain.routes.Contacts
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

/**
 * Contact API Routes using Ktor Type-Safe Routing
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
    val cashflowRepository by inject<CashflowRepository>()

    authenticateJwt {
        // ================================================================
        // CRUD OPERATIONS
        // ================================================================

        /**
         * GET /api/v1/contacts
         * List contacts with optional filters.
         *
         * Query parameters are automatically extracted from the route class.
         */
        get<Contacts> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val contacts = contactService.listContacts(
                tenantId = tenantId,
                isActive = route.active,
                peppolEnabled = route.peppolEnabled,
                searchQuery = route.search,
                limit = route.limit,
                offset = route.offset
            ).getOrElse { throw DokusException.InternalError("Failed to list contacts: ${it.message}") }

            call.respond(HttpStatusCode.OK, contacts)
        }

        /**
         * POST /api/v1/contacts
         * Create a new contact.
         */
        post<Contacts> {
            val tenantId = dokusPrincipal.requireTenantId()
            val request = call.receive<CreateContactRequest>()

            val contact = contactService.createContact(tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to create contact: ${it.message}") }

            // Create initial note if provided
            val initialNote = request.initialNote
            if (!initialNote.isNullOrBlank()) {
                val principal = dokusPrincipal
                contactNoteService.createNote(
                    tenantId = tenantId,
                    contactId = contact.id,
                    content = initialNote,
                    authorId = principal.userId,
                    authorName = principal.email
                )
            }

            call.respond(HttpStatusCode.Created, contact)
        }

        /**
         * GET /api/v1/contacts/customers
         * List customers only (ContactType.Customer or ContactType.Both).
         */
        get<Contacts.Customers> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            val contacts = contactService.listCustomers(tenantId, route.active, route.limit, route.offset)
                .getOrElse { throw DokusException.InternalError("Failed to list customers: ${it.message}") }

            call.respond(HttpStatusCode.OK, contacts)
        }

        /**
         * GET /api/v1/contacts/vendors
         * List vendors only (ContactType.Vendor or ContactType.Both).
         */
        get<Contacts.Vendors> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            val contacts = contactService.listVendors(tenantId, route.active, route.limit, route.offset)
                .getOrElse { throw DokusException.InternalError("Failed to list vendors: ${it.message}") }

            call.respond(HttpStatusCode.OK, contacts)
        }

        /**
         * GET /api/v1/contacts/summary
         * Get contact statistics for dashboard.
         */
        get<Contacts.Summary> {
            val tenantId = dokusPrincipal.requireTenantId()

            val stats = contactService.getContactStats(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get contact stats: ${it.message}") }

            call.respond(HttpStatusCode.OK, stats)
        }

        /**
         * GET /api/v1/contacts/{id}
         * Get a contact by ID.
         */
        get<Contacts.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.id)

            val contact = contactService.getContact(contactId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch contact: ${it.message}") }
                ?: throw DokusException.NotFound("Contact not found")

            call.respond(HttpStatusCode.OK, contact)
        }

        /**
         * PUT /api/v1/contacts/{id}
         * Update a contact.
         */
        put<Contacts.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.id)
            val request = call.receive<UpdateContactRequest>()

            val contact = contactService.updateContact(contactId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to update contact: ${it.message}") }

            call.respond(HttpStatusCode.OK, contact)
        }

        /**
         * DELETE /api/v1/contacts/{id}
         * Delete a contact.
         */
        delete<Contacts.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.id)

            contactService.deleteContact(contactId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete contact: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        // ================================================================
        // PEPPOL OPERATIONS
        // ================================================================

        /**
         * PATCH /api/v1/contacts/{id}/peppol
         * Update a contact's Peppol settings.
         */
        patch<Contacts.Id.Peppol> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.parent.id)
            val request = call.receive<UpdateContactPeppolRequest>()

            val contact = contactService.updateContactPeppol(
                contactId = contactId,
                tenantId = tenantId,
                peppolId = request.peppolId,
                peppolEnabled = request.peppolEnabled
            ).getOrElse { throw DokusException.InternalError("Failed to update contact Peppol settings: ${it.message}") }

            call.respond(HttpStatusCode.OK, contact)
        }

        // ================================================================
        // ACTIVITY OPERATIONS
        // ================================================================

        /**
         * GET /api/v1/contacts/{id}/activity
         * Get activity summary for a contact (counts and totals of invoices, bills, expenses).
         */
        get<Contacts.Id.Activity> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.parent.id)

            val activity = cashflowRepository.getContactActivitySummary(contactId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get contact activity: ${it.message}") }

            call.respond(HttpStatusCode.OK, activity)
        }

        // ================================================================
        // NOTES OPERATIONS
        // ================================================================

        /**
         * GET /api/v1/contacts/{id}/notes
         * List notes for a contact.
         */
        get<Contacts.Id.Notes> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val contactId = ContactId.parse(route.parent.id)

            val notes = contactNoteService.listNotes(contactId, tenantId, route.limit, route.offset)
                .getOrElse { throw DokusException.InternalError("Failed to list notes: ${it.message}") }

            call.respond(HttpStatusCode.OK, notes)
        }

        /**
         * POST /api/v1/contacts/{id}/notes
         * Add a note to a contact.
         */
        post<Contacts.Id.Notes> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val principal = dokusPrincipal
            val contactId = ContactId.parse(route.parent.id)
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
         * PUT /api/v1/contacts/{id}/notes/{noteId}
         * Update a note.
         */
        put<Contacts.Id.Notes.ById> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val noteId = ContactNoteId.parse(route.noteId)
            val request = call.receive<UpdateContactNoteRequest>()

            val note = contactNoteService.updateNote(noteId, tenantId, request.content)
                .getOrElse { throw DokusException.InternalError("Failed to update note: ${it.message}") }

            call.respond(HttpStatusCode.OK, note)
        }

        /**
         * DELETE /api/v1/contacts/{id}/notes/{noteId}
         * Delete a note.
         */
        delete<Contacts.Id.Notes.ById> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val noteId = ContactNoteId.parse(route.noteId)

            contactNoteService.deleteNote(noteId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete note: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}
