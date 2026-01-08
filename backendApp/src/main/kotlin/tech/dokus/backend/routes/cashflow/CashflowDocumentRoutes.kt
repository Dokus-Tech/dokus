package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Cashflow
import tech.dokus.foundation.backend.security.authenticateJwt

/**
 * @deprecated This route is being replaced. Use /api/v1/documents for the inbox
 * and /api/v1/cashflow/entries for the projection ledger.
 *
 * Returns an empty list for backwards compatibility while the old UI is being migrated.
 */
internal fun Route.cashflowDocumentRoutes() {
    authenticateJwt {
        get<Cashflow.CashflowDocuments> { _ ->
            // Return empty list - this endpoint is deprecated
            // The old CashflowScreen will be deleted after migration
            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse<FinancialDocumentDto>(
                    items = emptyList(),
                    total = 0L,
                    limit = 50,
                    offset = 0
                )
            )
        }
    }
}
