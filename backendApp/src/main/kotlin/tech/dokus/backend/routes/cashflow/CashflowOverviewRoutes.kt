package tech.dokus.backend.routes.cashflow

import tech.dokus.backend.security.requireTenantId

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.CashflowOverviewService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.routes.Cashflow
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal

/**
 * Cashflow Overview API Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/cashflow
 *
 * All routes require JWT authentication and tenant context.
 */
internal fun Route.cashflowOverviewRoutes() {
    val cashflowOverviewService by inject<CashflowOverviewService>()

    authenticateJwt {
        // GET /api/v1/cashflow/overview - Get cashflow overview
        get<Cashflow.Overview> { route ->
            val tenantId = requireTenantId()

            // Enums are used directly from route - no parsing needed
            val overview = cashflowOverviewService.getCashflowOverview(
                tenantId = tenantId,
                viewMode = route.viewMode,
                fromDate = route.fromDate,
                toDate = route.toDate,
                direction = route.direction,
                statuses = route.statuses
            ).getOrElse { throw DokusException.InternalError("Failed to calculate overview: ${it.message}") }

            call.respond(HttpStatusCode.OK, overview)
        }
    }
}
