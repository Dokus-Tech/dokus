package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.UpdateBillStatusRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * Bill API Routes (Cash-Out: Incoming Supplier Invoices)
 * Base path: /api/v1/cashflow/cash-out/bills
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.billRoutes() {
    val billService by inject<BillService>()

    route("/api/v1/cashflow/cash-out/bills") {
        authenticateJwt {

            // POST /api/v1/cashflow/cash-out/bills - Create bill
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateBillRequest>()

                val bill = billService.createBill(tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to create bill: ${it.message}") }

                call.respond(HttpStatusCode.Created, bill)
            }

            // GET /api/v1/cashflow/cash-out/bills/{billId} - Get bill by ID
            get("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val bill = billService.getBill(billId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch bill: ${it.message}") }
                    ?: throw DokusException.NotFound("Bill not found")

                call.respond(HttpStatusCode.OK, bill)
            }

            // GET /api/v1/cashflow/cash-out/bills - List bills with query params
            get {
                val tenantId = dokusPrincipal.requireTenantId()
                val status = call.parameters.billStatus
                val category = call.parameters.expenseCategory
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

                val bills = billService.listBills(
                    tenantId = tenantId,
                    status = status,
                    category = category,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                ).getOrElse { throw DokusException.InternalError("Failed to list bills: ${it.message}") }

                call.respond(HttpStatusCode.OK, bills)
            }

            // GET /api/v1/cashflow/cash-out/bills/overdue - List overdue bills
            get("/overdue") {
                val tenantId = dokusPrincipal.requireTenantId()

                val bills = billService.listOverdueBills(tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to list overdue bills: ${it.message}") }

                call.respond(HttpStatusCode.OK, bills)
            }

            // PUT /api/v1/cashflow/cash-out/bills/{billId} - Update bill
            put("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<CreateBillRequest>()

                val bill = billService.updateBill(billId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to update bill: ${it.message}") }

                call.respond(HttpStatusCode.OK, bill)
            }

            // PATCH /api/v1/cashflow/cash-out/bills/{billId}/status - Update bill status
            patch("/{billId}/status") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<UpdateBillStatusRequest>()

                val updated = billService.updateBillStatus(billId, tenantId, request.status)
                    .getOrElse { throw DokusException.InternalError("Failed to update bill status: ${it.message}") }

                if (!updated) {
                    throw DokusException.NotFound("Bill not found")
                }

                // Fetch and return updated bill
                val bill = billService.getBill(billId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to fetch bill: ${it.message}") }
                    ?: throw DokusException.NotFound("Bill not found")

                call.respond(HttpStatusCode.OK, bill)
            }

            // POST /api/v1/cashflow/cash-out/bills/{billId}/pay - Mark bill as paid
            post("/{billId}/pay") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<MarkBillPaidRequest>()

                val bill = billService.markBillPaid(billId, tenantId, request)
                    .getOrElse { throw DokusException.InternalError("Failed to mark bill as paid: ${it.message}") }

                call.respond(HttpStatusCode.OK, bill)
            }

            // DELETE /api/v1/cashflow/cash-out/bills/{billId} - Delete bill
            delete("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                billService.deleteBill(billId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to delete bill: ${it.message}") }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
