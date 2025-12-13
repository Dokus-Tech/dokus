package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Payment API.
 * Base path: /api/v1/payments
 */
@Serializable
@Resource("/api/v1/payments")
class Payments(
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val status: String? = null,
    val limit: Int = 50,
    val offset: Int = 0
) {
    /**
     * /api/v1/payments/{id} - Single payment operations
     */
    @Serializable
    @Resource("{id}")
    class Id(val parent: Payments = Payments(), val id: String) {
        /**
         * POST /api/v1/payments/{id}/refund - Refund a payment
         */
        @Serializable
        @Resource("refund")
        class Refund(val parent: Id)
    }

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
}
