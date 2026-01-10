package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Contact API.
 * Base path: /api/v1/contacts
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 * NOTE: ContactType was removed - roles are now derived from cashflow items.
 * NOTE: peppolEnabled filter removed - PEPPOL status is now in PeppolDirectoryCacheTable.
 */
@Serializable
@Resource("/api/v1/contacts")
class Contacts(
    val search: String? = null,
    val active: Boolean? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/contacts/customers - List customer contacts
     */
    @Serializable
    @Resource("customers")
    class Customers(
        val parent: Contacts = Contacts(),
        val active: Boolean = true,
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * GET /api/v1/contacts/vendors - List vendor contacts
     */
    @Serializable
    @Resource("vendors")
    class Vendors(
        val parent: Contacts = Contacts(),
        val active: Boolean = true,
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * GET /api/v1/contacts/summary - Contact statistics/summary
     */
    @Serializable
    @Resource("summary")
    class Summary(val parent: Contacts = Contacts())

    /**
     * /api/v1/contacts/{id} - Single contact operations
     * GET - Retrieve contact
     * PUT - Replace contact
     * PATCH - Partial update (including active status for activation/deactivation)
     * DELETE - Delete contact
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Contacts = Contacts(), val id: String) {

        /**
         * GET /api/v1/contacts/{id}/activity
         * Get activity summary for this contact (counts and totals of invoices, bills, expenses)
         */
        @Serializable
        @Resource("activity")
        class Activity(val parent: Id)

        // NOTE: PATCH /api/v1/contacts/{id}/peppol route removed
        // PEPPOL status is now managed via PeppolDirectoryCacheRepository

        /**
         * GET /api/v1/contacts/{id}/peppol-status
         * Get PEPPOL network status for this contact.
         * Returns cached status by default, use ?refresh=true to force lookup.
         */
        @Serializable
        @Resource("peppol-status")
        class PeppolStatus(val parent: Id, val refresh: Boolean = false)

        /**
         * POST /api/v1/contacts/{id}/merge-into/{targetId}
         * Merge this contact into another contact.
         * All cashflow items are reassigned to target, this contact is archived.
         */
        @Serializable
        @Resource("merge-into/{targetId}")
        class MergeInto(val parent: Id, val targetId: String)

        /**
         * GET/POST /api/v1/contacts/{id}/notes
         * GET - List notes for contact
         * POST - Create new note
         */
        @Serializable
        @Resource("notes")
        class Notes(
            val parent: Id,
            val limit: Int = 50,
            val offset: Int = 0
        ) {
            /**
             * GET/PUT/DELETE /api/v1/contacts/{id}/notes/{noteId}
             */
            @Serializable
            @Resource("{noteId}")
            class ById(val parent: Notes, val noteId: String)
        }
    }
}
