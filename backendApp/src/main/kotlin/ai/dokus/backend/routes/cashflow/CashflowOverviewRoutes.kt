package ai.dokus.backend.routes.cashflow

import ai.dokus.backend.services.cashflow.CashflowOverviewService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.routes.Cashflow
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject

/**
 * Cashflow Overview API Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/cashflow
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.cashflowOverviewRoutes() {
    val cashflowOverviewService by inject<CashflowOverviewService>()

    authenticateJwt {
        // GET /api/v1/cashflow/overview - Get cashflow overview
        get<Cashflow.Overview> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            val overview = cashflowOverviewService.getCashflowOverview(
                tenantId = tenantId,
                fromDate = route.fromDate,
                toDate = route.toDate
            ).getOrElse { throw DokusException.InternalError("Failed to calculate overview: ${it.message}") }

            call.respond(HttpStatusCode.OK, overview)
        }
    }
}
