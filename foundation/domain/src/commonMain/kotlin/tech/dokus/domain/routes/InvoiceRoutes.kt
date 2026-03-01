package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus

/**
 * Type-safe route definitions for Invoice API.
 * Base path: /api/v1/invoices
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 * Tenant filtering is enforced server-side on every query.
 */
@Serializable
@Resource("/api/v1/invoices")
class Invoices(
    val status: InvoiceStatus? = null,
    val direction: DocumentDirection? = null,
    val contactId: String? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/invoices/overdue - List overdue invoices
     */
    @Serializable
    @Resource("overdue")
    class Overdue(val parent: Invoices = Invoices())

    /**
     * /api/v1/invoices/{id} - Single invoice operations
     * GET - Retrieve invoice
     * PUT - Replace invoice
     * PATCH - Partial update
     * DELETE - Delete invoice
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Invoices = Invoices(), val id: String) {

        /**
         * GET/PATCH /api/v1/invoices/{id}/status
         * GET - Get current status
         * PATCH - Update status (e.g., DRAFT -> SENT, mark as sent)
         */
        @Serializable
        @Resource("status")
        class Status(val parent: Id)

        /**
         * GET/POST /api/v1/invoices/{id}/payments
         * GET - List payments for this invoice
         * POST - Record a new payment
         */
        @Serializable
        @Resource("payments")
        class Payments(val parent: Id)

        /**
         * POST /api/v1/invoices/{id}/pdf
         * Generate and upload a PDF export, returning a download URL.
         */
        @Serializable
        @Resource("pdf")
        class Pdf(val parent: Id)

        /**
         * GET/POST /api/v1/invoices/{id}/attachments
         * GET - List attachments
         * POST - Upload attachment
         */
        @Serializable
        @Resource("attachments")
        class Attachments(val parent: Id)
    }
}
