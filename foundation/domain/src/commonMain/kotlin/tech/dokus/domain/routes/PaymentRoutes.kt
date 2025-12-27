package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Payment API.
 * Base path: /api/v1/payments
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/payments")
class Payments(
    val status: String? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * GET /api/v1/payments/pending - List pending payments
     */
    @Serializable
    @Resource("pending")
    class Pending(
        val parent: Payments = Payments(),
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * GET /api/v1/payments/overdue - List overdue payments
     */
    @Serializable
    @Resource("overdue")
    class Overdue(
        val parent: Payments = Payments(),
        val limit: Int = 50,
        val offset: Int = 0
    )

    /**
     * /api/v1/payments/{id} - Single payment operations
     * GET - Get payment details
     * PATCH - Update payment
     * DELETE - Cancel payment
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Payments = Payments(), val id: String) {
        /**
         * GET/POST /api/v1/payments/{id}/refunds
         * GET - List refunds for this payment
         * POST - Create refund (partial or full)
         */
        @Serializable
        @Resource("refunds")
        class Refunds(val parent: Id)
    }
}
