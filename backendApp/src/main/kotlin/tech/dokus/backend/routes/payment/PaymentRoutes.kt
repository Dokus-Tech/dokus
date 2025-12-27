package tech.dokus.backend.routes.payment

import tech.dokus.domain.routes.Payments
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.slf4j.LoggerFactory

/**
 * Payment routes using Ktor Type-Safe Routing.
 * Base path: /api/v1/payments
 *
 * All routes require JWT authentication and tenant context.
 *
 * Note: This is a stub implementation. Payment functionality to be implemented.
 */
fun Route.paymentRoutes() {
    val logger = LoggerFactory.getLogger("PaymentRoutes")

    authenticateJwt {
        // GET /api/v1/payments - List payments
        get<Payments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            logger.info("Listing payments for tenant: $tenantId")

            // TODO: Implement payment listing
            call.respond(HttpStatusCode.OK, emptyList<Any>())
        }

        // GET /api/v1/payments/pending - List pending payments
        get<Payments.Pending> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            logger.info("Listing pending payments for tenant: $tenantId")

            // TODO: Implement pending payments listing
            call.respond(HttpStatusCode.OK, emptyList<Any>())
        }

        // GET /api/v1/payments/overdue - List overdue payments
        get<Payments.Overdue> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            logger.info("Listing overdue payments for tenant: $tenantId")

            // TODO: Implement overdue payments listing
            call.respond(HttpStatusCode.OK, emptyList<Any>())
        }

        // GET /api/v1/payments/{id} - Get payment by ID
        get<Payments.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            logger.info("Getting payment ${route.id} for tenant: $tenantId")

            // TODO: Implement payment retrieval
            call.respond(HttpStatusCode.NotFound, mapOf("message" to "Payment not found"))
        }

        // GET /api/v1/payments/{id}/refunds - List refunds for payment
        get<Payments.Id.Refunds> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            logger.info("Listing refunds for payment ${route.parent.id}, tenant: $tenantId")

            // TODO: Implement refunds listing
            call.respond(HttpStatusCode.OK, emptyList<Any>())
        }
    }
}
