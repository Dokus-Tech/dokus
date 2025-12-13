package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.ContactType
import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Contact API.
 * Base path: /api/v1/contacts
 */
@Serializable
@Resource("/api/v1/contacts")
class Contacts(
    val search: String? = null,
    val type: ContactType? = null,
    val activeOnly: Boolean? = null,
    val peppolEnabled: Boolean? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/contacts/peppol-enabled - List Peppol-enabled contacts
     */
    @Serializable
    @Resource("peppol-enabled")
    class PeppolEnabled(val parent: Contacts = Contacts())

    /**
     * GET /api/v1/contacts/customers - List customers only
     */
    @Serializable
    @Resource("customers")
    class Customers(
        val parent: Contacts = Contacts(),
        val activeOnly: Boolean = true,
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * GET /api/v1/contacts/vendors - List vendors only
     */
    @Serializable
    @Resource("vendors")
    class Vendors(
        val parent: Contacts = Contacts(),
        val activeOnly: Boolean = true,
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * GET /api/v1/contacts/stats - Get contact statistics
     */
    @Serializable
    @Resource("stats")
    class Stats(val parent: Contacts = Contacts())

    /**
     * /api/v1/contacts/{id} - Single contact operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Contacts = Contacts(), val id: String) {

        /**
         * POST /api/v1/contacts/{id}/deactivate
         */
        @Serializable
        @Resource("deactivate")
        class Deactivate(val parent: Id)

        /**
         * POST /api/v1/contacts/{id}/reactivate
         */
        @Serializable
        @Resource("reactivate")
        class Reactivate(val parent: Id)

        /**
         * PATCH /api/v1/contacts/{id}/peppol
         */
        @Serializable
        @Resource("peppol")
        class Peppol(val parent: Id)

        /**
         * GET/POST /api/v1/contacts/{id}/notes - Notes operations
         */
        @Serializable
        @Resource("notes")
        class Notes(
            val parent: Id,
            val limit: Int = 50,
            val offset: Int = 0
        ) {
            /**
             * PUT/DELETE /api/v1/contacts/{id}/notes/{noteId}
             */
            @Serializable
            @Resource("{noteId}")
            class ById(val parent: Notes, val noteId: String)
        }
    }
}
