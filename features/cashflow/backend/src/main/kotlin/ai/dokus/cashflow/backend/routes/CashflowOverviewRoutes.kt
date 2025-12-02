package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.CashflowOverviewService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Cashflow Overview API Routes
 * Base path: /api/v1/cashflow
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.cashflowOverviewRoutes() {
    val cashflowOverviewService by inject<CashflowOverviewService>()

    route("/api/v1/cashflow") {
        authenticateJwt {

            // GET /api/v1/cashflow/overview - Get cashflow overview
            get("/overview") {
                val tenantId = dokusPrincipal.requireTenantId()

                // Parse period or default to current month
                val fromDate = call.parameters.fromDate
                val toDate = call.parameters.toDate

                val overview = cashflowOverviewService.getCashflowOverview(
                    tenantId = tenantId,
                    fromDate = fromDate,
                    toDate = toDate
                ).getOrElse { throw DokusException.InternalError("Failed to calculate overview: ${it.message}") }

                call.respond(HttpStatusCode.OK, overview)
            }
        }
    }
}
