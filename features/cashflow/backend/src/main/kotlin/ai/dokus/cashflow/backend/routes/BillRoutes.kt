package ai.dokus.cashflow.backend.routes

import ai.dokus.cashflow.backend.service.BillService
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.UpdateBillStatusRequest
import ai.dokus.foundation.domain.routes.Bills
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.patch
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Bill API Routes using Ktor Type-Safe Routing (Cash-Out: Incoming Supplier Invoices)
 * Base path: /api/v1/cashflow/cash-out/bills
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
fun Route.billRoutes() {
    val billService by inject<BillService>()

    authenticateJwt {
        // GET /api/v1/cashflow/cash-out/bills - List bills with query params
        get<Bills> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit < 1 || route.limit > 200) {
                throw DokusException.BadRequest("Limit must be between 1 and 200")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val bills = billService.listBills(
                tenantId = tenantId,
                status = route.status,
                category = route.category,
                fromDate = route.fromDate,
                toDate = route.toDate,
                limit = route.limit,
                offset = route.offset
            ).getOrElse { throw DokusException.InternalError("Failed to list bills: ${it.message}") }

            call.respond(HttpStatusCode.OK, bills)
        }

        // POST /api/v1/cashflow/cash-out/bills - Create bill
        post<Bills> {
            val tenantId = dokusPrincipal.requireTenantId()
            val request = call.receive<CreateBillRequest>()

            val bill = billService.createBill(tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to create bill: ${it.message}") }

            call.respond(HttpStatusCode.Created, bill)
        }

        // GET /api/v1/cashflow/cash-out/bills/overdue - List overdue bills
        get<Bills.Overdue> {
            val tenantId = dokusPrincipal.requireTenantId()

            val bills = billService.listOverdueBills(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to list overdue bills: ${it.message}") }

            call.respond(HttpStatusCode.OK, bills)
        }

        // GET /api/v1/cashflow/cash-out/bills/{id} - Get bill by ID
        get<Bills.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val billId = BillId(Uuid.parse(route.id))

            val bill = billService.getBill(billId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to fetch bill: ${it.message}") }
                ?: throw DokusException.NotFound("Bill not found")

            call.respond(HttpStatusCode.OK, bill)
        }

        // PUT /api/v1/cashflow/cash-out/bills/{id} - Update bill
        put<Bills.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val billId = BillId(Uuid.parse(route.id))
            val request = call.receive<CreateBillRequest>()

            val bill = billService.updateBill(billId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to update bill: ${it.message}") }

            call.respond(HttpStatusCode.OK, bill)
        }

        // DELETE /api/v1/cashflow/cash-out/bills/{id} - Delete bill
        delete<Bills.Id> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val billId = BillId(Uuid.parse(route.id))

            billService.deleteBill(billId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to delete bill: ${it.message}") }

            call.respond(HttpStatusCode.NoContent)
        }

        // PATCH /api/v1/cashflow/cash-out/bills/{id}/status - Update bill status
        patch<Bills.Id.Status> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val billId = BillId(Uuid.parse(route.parent.id))
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

        // POST /api/v1/cashflow/cash-out/bills/{id}/pay - Mark bill as paid
        post<Bills.Id.Pay> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
            val billId = BillId(Uuid.parse(route.parent.id))
            val request = call.receive<MarkBillPaidRequest>()

            val bill = billService.markBillPaid(billId, tenantId, request)
                .getOrElse { throw DokusException.InternalError("Failed to mark bill as paid: ${it.message}") }

            call.respond(HttpStatusCode.OK, bill)
        }
    }
}
