package ai.dokus.cashflow.backend.routes

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CashflowOverview
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Cashflow Overview API Routes
 * Base path: /api/v1/cashflow
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.cashflowOverviewRoutes() {
    val logger = LoggerFactory.getLogger("CashflowOverviewRoutes")

    route("/api/v1/cashflow") {

        // GET /api/v1/cashflow/overview - Get cashflow overview
        get("/overview") {
            withTenant { tenantId, _ ->
                val fromDate = call.parameters.fromDate
                    ?: throw DokusException.Validation.Other

                val toDate = call.parameters.toDate
                    ?: throw DokusException.Validation.Other

                logger.info("Getting cashflow overview for tenant: $tenantId (from=$fromDate, to=$toDate)")

                // TODO: Implement overview calculation when service is available
                val overview = CashflowOverview(
                    totalIncome = Money.ZERO,
                    totalExpenses = Money.ZERO,
                    netCashflow = Money.ZERO,
                    pendingInvoices = Money.ZERO,
                    overdueInvoices = Money.ZERO,
                    invoiceCount = 0,
                    expenseCount = 0
                )

                call.respond(HttpStatusCode.OK, overview)
            }
        }
    }
}
