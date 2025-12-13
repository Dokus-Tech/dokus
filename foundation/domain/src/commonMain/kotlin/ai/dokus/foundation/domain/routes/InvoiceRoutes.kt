package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.InvoiceStatus
import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Invoice API.
 * Base path: /api/v1/invoices
 *
 * Uses Ktor Resources nested class pattern where child classes
 * reference their parent for path inheritance.
 */
@Serializable
@Resource("/api/v1/invoices")
class Invoices(
    val status: InvoiceStatus? = null,
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
     * POST /api/v1/invoices/calculate-totals - Calculate invoice totals
     */
    @Serializable
    @Resource("calculate-totals")
    class CalculateTotals(val parent: Invoices = Invoices())

    /**
     * /api/v1/invoices/{id} - Single invoice operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Invoices = Invoices(), val id: String) {

        /**
         * PATCH /api/v1/invoices/{id}/status - Update invoice status
         */
        @Serializable
        @Resource("status")
        class Status(val parent: Id)

        /**
         * POST /api/v1/invoices/{id}/payments - Record payment
         */
        @Serializable
        @Resource("payments")
        class Payments(val parent: Id)

        /**
         * POST /api/v1/invoices/{id}/send-email - Send invoice via email
         */
        @Serializable
        @Resource("send-email")
        class SendEmail(val parent: Id)

        /**
         * POST /api/v1/invoices/{id}/mark-sent - Mark invoice as sent
         */
        @Serializable
        @Resource("mark-sent")
        class MarkSent(val parent: Id)

        /**
         * GET/POST /api/v1/invoices/{id}/attachments - List or upload attachments
         */
        @Serializable
        @Resource("attachments")
        class Attachments(val parent: Id)
    }
}
