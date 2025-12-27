package tech.dokus.backend.routes.cashflow

import ai.dokus.foundation.database.repository.cashflow.CashflowRepository
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.routes.Cashflow
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.server.resources.get
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

/**
 * Cashflow Document Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/cashflow/documents
 *
 * All routes require JWT authentication and tenant context.
 */
internal fun Route.cashflowDocumentRoutes() {
    val repository by inject<CashflowRepository>()
    val logger = LoggerFactory.getLogger("CashflowDocumentRoutes")

    authenticateJwt {
        // GET /api/v1/cashflow/documents - List cashflow documents
        get<Cashflow.CashflowDocuments> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val page = repository.listDocuments(
                tenantId = tenantId,
                fromDate = route.fromDate,
                toDate = route.toDate,
                limit = route.limit,
                offset = route.offset
            )
                .onSuccess {
                    logger.info("Fetched ${it.items.size} cashflow documents (offset=${route.offset}, limit=${route.limit}, total=${it.total})")
                }
                .onFailure {
                    logger.error("Failed to fetch cashflow documents for tenant=$tenantId", it)
                    throw DokusException.InternalError("Failed to list cashflow documents: ${it.message}")
                }
                .getOrThrow()

            call.respond(HttpStatusCode.OK, page)
        }
    }
}
