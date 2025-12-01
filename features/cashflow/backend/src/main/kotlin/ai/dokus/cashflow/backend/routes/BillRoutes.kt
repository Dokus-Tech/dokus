package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.repository.BillRepository
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
import org.slf4j.LoggerFactory

/**
 * Bill API Routes (Cash-Out: Incoming Supplier Invoices)
 * Base path: /api/v1/cashflow/cash-out/bills
 *
 * All routes require JWT authentication and tenant context.
 */
fun Route.billRoutes() {
    val billRepository by inject<BillRepository>()
    val logger = LoggerFactory.getLogger("BillRoutes")

    route("/api/v1/cashflow/cash-out/bills") {
        authenticateJwt {

            // POST /api/v1/cashflow/cash-out/bills - Create bill
            post {
                val tenantId = dokusPrincipal.requireTenantId()
                val request = call.receive<CreateBillRequest>()
                logger.info("Creating bill for tenant: $tenantId, supplier: ${request.supplierName}")

                val bill = billRepository.createBill(tenantId, request)
                    .onSuccess { logger.info("Bill created: ${it.id}") }
                    .onFailure {
                        logger.error("Failed to create bill for tenant: $tenantId", it)
                        throw DokusException.InternalError("Failed to create bill: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.Created, bill)
            }

            // GET /api/v1/cashflow/cash-out/bills/{billId} - Get bill by ID
            get("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                logger.info("Fetching bill: $billId for tenant: $tenantId")

                val bill = billRepository.getBill(billId, tenantId)
                    .onFailure {
                        logger.error("Failed to fetch bill: $billId", it)
                        throw DokusException.InternalError("Failed to fetch bill: ${it.message}")
                    }
                    .getOrThrow()
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

                logger.info("Listing bills for tenant: $tenantId (status=$status, category=$category, limit=$limit, offset=$offset)")

                val bills = billRepository.listBills(
                    tenantId = tenantId,
                    status = status,
                    category = category,
                    fromDate = fromDate,
                    toDate = toDate,
                    limit = limit,
                    offset = offset
                )
                    .onSuccess { logger.info("Retrieved ${it.items.size} bills (total=${it.total})") }
                    .onFailure {
                        logger.error("Failed to list bills for tenant: $tenantId", it)
                        throw DokusException.InternalError("Failed to list bills: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, bills)
            }

            // GET /api/v1/cashflow/cash-out/bills/overdue - List overdue bills
            get("/overdue") {
                val tenantId = dokusPrincipal.requireTenantId()
                logger.info("Listing overdue bills for tenant: $tenantId")

                val bills = billRepository.listOverdueBills(tenantId)
                    .onSuccess { logger.info("Retrieved ${it.size} overdue bills") }
                    .onFailure {
                        logger.error("Failed to list overdue bills for tenant: $tenantId", it)
                        throw DokusException.InternalError("Failed to list overdue bills: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, bills)
            }

            // PUT /api/v1/cashflow/cash-out/bills/{billId} - Update bill
            put("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<CreateBillRequest>()
                logger.info("Updating bill: $billId")

                val bill = billRepository.updateBill(billId, tenantId, request)
                    .onSuccess { logger.info("Bill updated: $billId") }
                    .onFailure {
                        logger.error("Failed to update bill: $billId", it)
                        throw DokusException.InternalError("Failed to update bill: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, bill)
            }

            // PATCH /api/v1/cashflow/cash-out/bills/{billId}/status - Update bill status
            patch("/{billId}/status") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<UpdateBillStatusRequest>()
                logger.info("Updating bill status: $billId -> ${request.status}")

                val updated = billRepository.updateBillStatus(billId, tenantId, request.status)
                    .onSuccess { logger.info("Bill status updated: $billId -> ${request.status}") }
                    .onFailure {
                        logger.error("Failed to update bill status: $billId", it)
                        throw DokusException.InternalError("Failed to update bill status: ${it.message}")
                    }
                    .getOrThrow()

                if (!updated) {
                    throw DokusException.NotFound("Bill not found")
                }

                // Fetch and return updated bill
                val bill = billRepository.getBill(billId, tenantId).getOrThrow()
                    ?: throw DokusException.NotFound("Bill not found")

                call.respond(HttpStatusCode.OK, bill)
            }

            // POST /api/v1/cashflow/cash-out/bills/{billId}/pay - Mark bill as paid
            post("/{billId}/pay") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                val request = call.receive<MarkBillPaidRequest>()
                logger.info("Marking bill as paid: $billId")

                val bill = billRepository.markBillPaid(billId, tenantId, request)
                    .onSuccess { logger.info("Bill marked as paid: $billId") }
                    .onFailure {
                        logger.error("Failed to mark bill as paid: $billId", it)
                        throw DokusException.InternalError("Failed to mark bill as paid: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.OK, bill)
            }

            // DELETE /api/v1/cashflow/cash-out/bills/{billId} - Delete bill
            delete("/{billId}") {
                val tenantId = dokusPrincipal.requireTenantId()
                val billId = call.parameters.billId
                    ?: throw DokusException.BadRequest("Bill ID is required")

                logger.info("Deleting bill: $billId")

                billRepository.deleteBill(billId, tenantId)
                    .onSuccess { logger.info("Bill deleted: $billId") }
                    .onFailure {
                        logger.error("Failed to delete bill: $billId", it)
                        throw DokusException.InternalError("Failed to delete bill: ${it.message}")
                    }
                    .getOrThrow()

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
