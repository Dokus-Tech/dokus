package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.CashflowOverviewService
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowViewMode
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
            val tenantId = dokusPrincipal.requireTenantId()

            // Parse viewMode filter
            val viewMode = route.viewMode?.let { vm ->
                try {
                    CashflowViewMode.valueOf(vm.replaceFirstChar { it.uppercase() })
                } catch (_: IllegalArgumentException) {
                    throw DokusException.BadRequest("Invalid viewMode: $vm. Must be Upcoming or History")
                }
            }

            // Parse direction filter
            val direction = route.direction?.let { dir ->
                try {
                    CashflowDirection.valueOf(dir)
                } catch (_: IllegalArgumentException) {
                    throw DokusException.BadRequest("Invalid direction: $dir. Must be IN or OUT")
                }
            }

            // Parse multi-status filter (comma-separated)
            val statuses = route.status?.split(",")?.mapNotNull { s ->
                try {
                    CashflowEntryStatus.valueOf(s.trim().replaceFirstChar { it.uppercase() })
                } catch (_: IllegalArgumentException) {
                    null // Skip invalid statuses silently
                }
            }?.ifEmpty { null }

            val overview = cashflowOverviewService.getCashflowOverview(
                tenantId = tenantId,
                viewMode = viewMode,
                fromDate = route.fromDate,
                toDate = route.toDate,
                direction = direction,
                statuses = statuses
            ).getOrElse { throw DokusException.InternalError("Failed to calculate overview: ${it.message}") }

            call.respond(HttpStatusCode.OK, overview)
        }
    }
}
