package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.cashflow.BankStatementMatchingService
import tech.dokus.backend.services.cashflow.AutoPaymentService
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.services.cashflow.CashflowPaymentService
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.UndoAutoPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Cashflow
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_PAGE_SIZE = 200

/**
 * Cashflow Entries API Routes using Ktor Type-Safe Routing
 * Base path: /api/v1/cashflow/entries
 *
 * Provides the projection ledger for cashflow (Cash-In/Cash-Out).
 * Entries are created when documents are confirmed.
 *
 * All routes require JWT authentication and tenant context.
 */
@OptIn(ExperimentalUuidApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "ThrowsCount")
internal fun Route.cashflowEntriesRoutes() {
    val cashflowEntriesService by inject<CashflowEntriesService>()
    val bankStatementMatchingService by inject<BankStatementMatchingService>()
    val cashflowPaymentService by inject<CashflowPaymentService>()
    val autoPaymentService by inject<AutoPaymentService>()

    authenticateJwt {
        // GET /api/v1/cashflow/entries - List cashflow entries with filters
        get<Cashflow.Entries> { route ->
            val tenantId = requireTenantId()

            if (route.limit !in 1..MAX_PAGE_SIZE) {
                throw DokusException.BadRequest("Limit must be between 1 and $MAX_PAGE_SIZE")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            // If entryId is specified, return single-item result for deep link
            val entryId = route.entryId
            if (entryId != null) {
                val entry = cashflowEntriesService.getEntry(entryId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to get entry: ${it.message}") }
                    ?: throw DokusException.NotFound("Cashflow entry not found: $entryId")

                call.respond(
                    HttpStatusCode.OK,
                    PaginatedResponse(
                        items = listOf(entry),
                        total = 1L,
                        limit = 1,
                        offset = 0
                    )
                )
                return@get
            }

            // List entries with filters (enums are used directly from route)
            val entries = cashflowEntriesService.listEntries(
                tenantId = tenantId,
                viewMode = route.viewMode,
                fromDate = route.fromDate,
                toDate = route.toDate,
                direction = route.direction,
                statuses = route.statuses
            ).getOrElse { throw DokusException.InternalError("Failed to list entries: ${it.message}") }

            // Apply sourceType filter in-memory (repository doesn't support it yet)
            val filteredEntries = if (route.sourceType != null) {
                entries.filter { it.sourceType == route.sourceType }
            } else {
                entries
            }

            // Apply pagination
            val paginatedEntries = filteredEntries
                .drop(route.offset)
                .take(route.limit)

            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse(
                    items = paginatedEntries,
                    total = filteredEntries.size.toLong(),
                    limit = route.limit,
                    offset = route.offset
                )
            )
        }

        // GET /api/v1/cashflow/entries/{id} - Get single entry
        get<Cashflow.Entries.Id> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }

            val entry = cashflowEntriesService.getEntry(entryId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get entry: ${it.message}") }
                ?: throw DokusException.NotFound("Cashflow entry not found: ${route.id}")

            call.respond(HttpStatusCode.OK, entry)
        }

        // POST /api/v1/cashflow/entries/{id}/payments - Record payment
        post<Cashflow.Entries.Id.Payments> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.parent.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }

            val request = call.receive<CashflowPaymentRequest>()

            // Validate amount is positive
            if (request.amount.minor <= 0) {
                throw DokusException.BadRequest("Payment amount must be positive")
            }

            val updatedEntry = cashflowPaymentService.recordPayment(
                tenantId = tenantId,
                entryId = entryId,
                request = request
            ).getOrElse { error ->
                throw (error as? DokusException
                    ?: DokusException.InternalError("Failed to record payment: ${error.message}"))
            }

            call.respond(HttpStatusCode.OK, updatedEntry)
        }

        // GET /api/v1/cashflow/entries/{id}/payment-candidates - Candidate imported transactions
        get<Cashflow.Entries.Id.PaymentCandidates> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.parent.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }

            val response = runSuspendCatching {
                bankStatementMatchingService.getPaymentCandidates(
                    tenantId = tenantId,
                    cashflowEntryId = entryId
                )
            }.getOrElse {
                throw DokusException.BadRequest("Failed to load payment candidates: ${it.message}")
            }
            call.respond(HttpStatusCode.OK, response)
        }

        // GET /api/v1/cashflow/entries/{id}/auto-payment-status
        get<Cashflow.Entries.Id.AutoPaymentStatus> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.parent.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }

            val status = autoPaymentService.getAutoPaymentStatus(
                tenantId = tenantId,
                entryId = entryId
            )
            call.respond(HttpStatusCode.OK, status)
        }

        // POST /api/v1/cashflow/entries/{id}/undo-auto-payment
        post<Cashflow.Entries.Id.UndoAutoPayment> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.parent.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }
            val request = try {
                call.receive<UndoAutoPaymentRequest>()
            } catch (_: Exception) {
                UndoAutoPaymentRequest()
            }

            val updatedEntry = autoPaymentService.undoAutoPayment(
                tenantId = tenantId,
                entryId = entryId,
                actorUserId = dokusPrincipal.userId,
                reason = request.reason
            ).getOrElse { error ->
                throw (error as? DokusException
                    ?: DokusException.InternalError("Failed to undo auto payment: ${error.message}"))
            }

            call.respond(HttpStatusCode.OK, updatedEntry)
        }

        // POST /api/v1/cashflow/entries/{id}/cancel - Cancel entry
        post<Cashflow.Entries.Id.Cancel> { route ->
            val tenantId = requireTenantId()
            val entryId = try {
                CashflowEntryId(Uuid.parse(route.parent.id))
            } catch (_: Exception) {
                throw DokusException.BadRequest("Invalid entry ID format")
            }

            // Optional request body for reason
            val request = try {
                call.receive<CancelEntryRequest>()
            } catch (_: Exception) {
                CancelEntryRequest() // Empty request is OK
            }

            // Verify entry exists and is cancellable
            val entry = cashflowEntriesService.getEntry(entryId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get entry: ${it.message}") }
                ?: throw DokusException.NotFound("Cashflow entry not found: ${route.parent.id}")

            if (entry.status == CashflowEntryStatus.Cancelled) {
                throw DokusException.BadRequest("Entry is already cancelled")
            }
            if (entry.status == CashflowEntryStatus.Paid) {
                throw DokusException.BadRequest("Cannot cancel a paid entry")
            }

            cashflowEntriesService.cancel(entryId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to cancel entry: ${it.message}") }

            // Return updated entry
            val updatedEntry = cashflowEntriesService.getEntry(entryId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get updated entry: ${it.message}") }
                ?: throw DokusException.NotFound("Cashflow entry not found after update")

            call.respond(HttpStatusCode.OK, updatedEntry)
        }
    }
}
