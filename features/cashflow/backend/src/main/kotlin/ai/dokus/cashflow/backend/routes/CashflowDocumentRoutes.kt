package ai.dokus.cashflow.backend.routes

import ai.dokus.foundation.database.repository.cashflow.CashflowRepository
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

fun Route.cashflowDocumentRoutes() {
    val repository by inject<CashflowRepository>()
    val logger = LoggerFactory.getLogger("CashflowDocumentRoutes")

    route("/api/v1/cashflow") {
        authenticateJwt {
            get("/documents") {
                val tenantId = dokusPrincipal.requireTenantId()
                val fromDate = call.parameters.fromDate
                val toDate = call.parameters.toDate
                val limit = call.parameters.limit
                val offset = call.parameters.offset

                if (limit < 1 || limit > 200) {
                    throw DokusException.BadRequest("Limit must be between 1 and 200")
                }
                if (offset < 0) {
                    throw DokusException.BadRequest("Offset must be non-negative")
                }

                val page = repository.listDocuments(
                    tenantId = tenantId,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                )
                    .onSuccess {
                        logger.info("Fetched ${it.items.size} cashflow documents (offset=$offset, limit=$limit, total=${it.total})")
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
}
