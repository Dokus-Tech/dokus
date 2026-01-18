package tech.dokus.backend.routes.cashflow

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Cashflow
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
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

    authenticateJwt {
        // GET /api/v1/cashflow/entries - List cashflow entries with filters
        get<Cashflow.Entries> { route ->
            val tenantId = dokusPrincipal.requireTenantId()

            if (route.limit !in 1..MAX_PAGE_SIZE) {
                throw DokusException.BadRequest("Limit must be between 1 and $MAX_PAGE_SIZE")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

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

            // Parse source type filter
            val sourceType = route.sourceType?.let { st ->
                try {
                    CashflowSourceType.valueOf(st)
                } catch (_: IllegalArgumentException) {
                    throw DokusException.BadRequest("Invalid sourceType: $st. Must be Invoice, Bill, or Expense")
                }
            }

            // If entryId is specified, return single-item result for deep link
            val routeEntryId = route.entryId
            if (routeEntryId != null) {
                val entryId = try {
                    CashflowEntryId(Uuid.parse(routeEntryId))
                } catch (_: Exception) {
                    throw DokusException.BadRequest("Invalid entryId format")
                }

                val entry = cashflowEntriesService.getEntry(entryId, tenantId)
                    .getOrElse { throw DokusException.InternalError("Failed to get entry: ${it.message}") }
                    ?: throw DokusException.NotFound("Cashflow entry not found: ${route.entryId}")

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

            // List entries with filters
            val entries = cashflowEntriesService.listEntries(
                tenantId = tenantId,
                viewMode = viewMode,
                fromDate = route.fromDate,
                toDate = route.toDate,
                direction = direction,
                statuses = statuses
            ).getOrElse { throw DokusException.InternalError("Failed to list entries: ${it.message}") }

            // Apply sourceType filter in-memory (repository doesn't support it yet)
            val filteredEntries = if (sourceType != null) {
                entries.filter { it.sourceType == sourceType }
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
            val tenantId = dokusPrincipal.requireTenantId()
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
            val tenantId = dokusPrincipal.requireTenantId()
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

            cashflowEntriesService.recordPayment(entryId, tenantId, request.amount)
                .getOrElse { throw DokusException.InternalError("Failed to record payment: ${it.message}") }

            // Return updated entry
            val updatedEntry = cashflowEntriesService.getEntry(entryId, tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get updated entry: ${it.message}") }
                ?: throw DokusException.NotFound("Cashflow entry not found after update")

            call.respond(HttpStatusCode.OK, updatedEntry)
        }

        // POST /api/v1/cashflow/entries/{id}/cancel - Cancel entry
        post<Cashflow.Entries.Id.Cancel> { route ->
            val tenantId = dokusPrincipal.requireTenantId()
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
