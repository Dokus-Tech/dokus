package ai.dokus.backend.routes.payment

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/**
 * Registers all Payment REST API routes.
 *
 * Routes registered:
 * - /api/v1/payments - Payment listing
 * - /api/v1/payments/pending - Pending payments
 * - /api/v1/payments/overdue - Overdue payments
 * - /api/v1/payments/{id} - Payment details
 * - /api/v1/payments/{id}/refunds - Payment refunds
 *
 * Note: This is a stub implementation. Payment functionality to be implemented.
 */
fun Application.configurePaymentRoutes() {
    routing {
        paymentRoutes()
    }
}
