package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Peppol e-invoicing API.
 * Base path: /api/v1/peppol
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
     * GET/PUT/DELETE /api/v1/peppol/settings - Peppol settings operations
     */
    @Serializable
    @Resource("settings")
    class Settings(val parent: Peppol = Peppol()) {
        /**
         * POST /api/v1/peppol/settings/test - Test Peppol connection
         */
        @Serializable
        @Resource("test")
        class Test(val parent: Settings)
    }

    /**
     * POST /api/v1/peppol/verify - Verify Peppol recipient
     */
    @Serializable
    @Resource("verify")
    class Verify(val parent: Peppol = Peppol())

    /**
     * /api/v1/peppol/send - Send operations
     */
    @Serializable
    @Resource("send")
    class Send(val parent: Peppol = Peppol()) {
        /**
         * POST /api/v1/peppol/send/invoice/{invoiceId} - Send invoice via Peppol
         */
        @Serializable
        @Resource("invoice/{invoiceId}")
        class Invoice(val parent: Send, val invoiceId: String)

        /**
         * POST /api/v1/peppol/send/validate/{invoiceId} - Validate invoice for Peppol
         */
        @Serializable
        @Resource("validate/{invoiceId}")
        class Validate(val parent: Send, val invoiceId: String)
    }

    /**
     * /api/v1/peppol/inbox - Inbox operations
     */
    @Serializable
    @Resource("inbox")
    class Inbox(val parent: Peppol = Peppol()) {
        /**
         * POST /api/v1/peppol/inbox/poll - Poll Peppol inbox
         */
        @Serializable
        @Resource("poll")
        class Poll(val parent: Inbox)
    }

    /**
     * GET /api/v1/peppol/transmissions - List Peppol transmissions
     */
    @Serializable
    @Resource("transmissions")
    class Transmissions(
        val parent: Peppol = Peppol(),
        val direction: PeppolTransmissionDirection? = null,
        val status: PeppolStatus? = null,
        val limit: Int = 50,
        val offset: Int = 0
    ) {
        /**
         * GET /api/v1/peppol/transmissions/invoice/{invoiceId}
         */
        @Serializable
        @Resource("invoice/{invoiceId}")
        class ByInvoice(val parent: Transmissions, val invoiceId: String)
    }
}
