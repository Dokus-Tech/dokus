package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.FromMediaService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateBillFromMediaRequest
import ai.dokus.foundation.domain.model.CreateExpenseFromMediaRequest
import ai.dokus.foundation.domain.model.CreateInvoiceFromMediaRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

/**
 * From-Media Creation Routes
 *
 * These routes create financial entities (invoices, expenses, bills) from
 * processed media extraction data. The media must be in PROCESSED status
 * with extraction data available.
 *
 * Flow:
 * 1. Client uploads document to /api/v1/media
 * 2. AI processes the document and extracts data
 * 3. Client calls these routes with mediaId and optional corrections
 * 4. Entity is created from extraction data + corrections
 * 5. Media is attached to the created entity
 */
fun Route.fromMediaRoutes() {
    val fromMediaService by inject<FromMediaService>()

    // Invoice from media: POST /api/v1/cashflow/cash-in/invoices/from-media/{mediaId}
    route("/api/v1/cashflow/cash-in/invoices/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateInvoiceFromMediaRequest>()

                val result = fromMediaService.createInvoiceFromMedia(
                    mediaId = mediaId,
                    tenantId = tenantId,
                    clientId = request.clientId,
                    corrections = request.corrections
                ).getOrElse { throw DokusException.InternalError("Failed to create invoice: ${it.message}") }

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = result.entity,
                    mediaId = result.mediaId.toString(),
                    createdFrom = result.createdFrom
                ))
            }
        }
    }

    // Expense from media: POST /api/v1/cashflow/cash-out/expenses/from-media/{mediaId}
    route("/api/v1/cashflow/cash-out/expenses/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateExpenseFromMediaRequest>()

                val result = fromMediaService.createExpenseFromMedia(
                    mediaId = mediaId,
                    tenantId = tenantId,
                    corrections = request.corrections
                ).getOrElse { throw DokusException.InternalError("Failed to create expense: ${it.message}") }

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = result.entity,
                    mediaId = result.mediaId.toString(),
                    createdFrom = result.createdFrom
                ))
            }
        }
    }

    // Bill from media: POST /api/v1/cashflow/cash-out/bills/from-media/{mediaId}
    route("/api/v1/cashflow/cash-out/bills/from-media/{mediaId}") {
        authenticateJwt {
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val mediaId = call.parameters.mediaId
                    ?: throw DokusException.BadRequest("Media ID is required")
                val request = call.receive<CreateBillFromMediaRequest>()

                val result = fromMediaService.createBillFromMedia(
                    mediaId = mediaId,
                    tenantId = tenantId,
                    corrections = request.corrections
                ).getOrElse { throw DokusException.InternalError("Failed to create bill: ${it.message}") }

                call.respond(HttpStatusCode.Created, CreatedFromMediaResult(
                    entity = result.entity,
                    mediaId = result.mediaId.toString(),
                    createdFrom = result.createdFrom
                ))
            }
        }
    }
}

/**
 * Response for creating an entity from media
 */
@Serializable
data class CreatedFromMediaResult<T>(
    val entity: T,
    val mediaId: String,
    val createdFrom: String
)
