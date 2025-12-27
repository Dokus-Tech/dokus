package tech.dokus.domain.routes

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Peppol e-invoicing API.
 * Base path: /api/v1/peppol
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/peppol")
class Peppol {
    /**
     * GET /api/v1/peppol/providers - List available Peppol providers
     */
    @Serializable
    @Resource("providers")
    class Providers(val parent: Peppol = Peppol())

    /**
     * GET/PUT/DELETE /api/v1/peppol/settings
     * GET - Get Peppol settings
     * PUT - Update Peppol settings
     * DELETE - Remove Peppol settings
     */
    @Serializable
    @Resource("settings")
    class Settings(val parent: Peppol = Peppol()) {
        /**
         * POST /api/v1/peppol/settings/connection-tests
         * Creates a connection test (tests Peppol provider connectivity)
         */
        @Serializable
        @Resource("connection-tests")
        class ConnectionTests(val parent: Settings)

        /**
         * POST /api/v1/peppol/settings/connect
         * Connects tenant to Recommand by matching (and optionally creating) company by VAT.
         */
        @Serializable
        @Resource("connect")
        class Connect(val parent: Settings)
    }

    /**
     * GET/POST /api/v1/peppol/recipient-validations
     * GET - List past recipient validations
     * POST - Validate a Peppol recipient (creates validation record)
     */
    @Serializable
    @Resource("recipient-validations")
    class RecipientValidations(val parent: Peppol = Peppol())

    /**
     * GET/POST /api/v1/peppol/transmissions
     * GET - List Peppol transmissions (with filters)
     * POST - Send document via Peppol (creates transmission)
     */
    @Serializable
    @Resource("transmissions")
    class Transmissions(
        val parent: Peppol = Peppol(),
        val direction: PeppolTransmissionDirection? = null,
        val status: PeppolStatus? = null,
        val invoiceId: String? = null,
        val limit: Int = 50,
        val offset: Int = 0
    ) {
        /**
         * GET /api/v1/peppol/transmissions/{id}
         * Get transmission details
         */
        @Serializable
        @Resource("{id}")
        class Id(val parent: Transmissions, val id: String)
    }

    /**
     * GET/POST /api/v1/peppol/invoice-validations
     * POST - Validate invoice for Peppol compliance (creates validation result)
     */
    @Serializable
    @Resource("invoice-validations")
    class InvoiceValidations(
        val parent: Peppol = Peppol(),
        val invoiceId: String? = null
    )

    /**
     * GET/POST /api/v1/peppol/inbox
     * GET - Get inbox messages
     */
    @Serializable
    @Resource("inbox")
    class Inbox(val parent: Peppol = Peppol()) {
        /**
         * POST /api/v1/peppol/inbox/syncs
         * Creates a sync operation (polls inbox for new messages)
         */
        @Serializable
        @Resource("syncs")
        class Syncs(val parent: Inbox)
    }
}
