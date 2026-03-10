package tech.dokus.backend.routes.banking

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.banking.BankingService
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.model.IgnoreTransactionRequest
import tech.dokus.domain.model.LinkTransactionRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Banking
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = loggerFor("BankingRoutes")
private const val MAX_PAGE_SIZE = 200

fun Application.configureBankingRouting() {
    logger.info("Configuring banking routes...")

    routing {
        bankingRoutes()
    }

    logger.info("Banking routes configured")
}

@OptIn(ExperimentalUuidApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "ThrowsCount")
internal fun Route.bankingRoutes() {
    val bankingService by inject<BankingService>()

    authenticateJwt {
        // GET /api/v1/banking/accounts
        get<Banking.Accounts> {
            val tenantId = requireTenantId()
            val accounts = bankingService.listAccounts(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to list accounts: ${it.message}") }
            call.respond(HttpStatusCode.OK, accounts)
        }

        // GET /api/v1/banking/accounts/summary
        get<Banking.AccountsSummary> {
            val tenantId = requireTenantId()
            val summary = bankingService.getAccountSummary(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get account summary: ${it.message}") }
            call.respond(HttpStatusCode.OK, summary)
        }

        // GET /api/v1/banking/accounts/balance-history
        get<Banking.AccountsBalanceHistory> { route ->
            val tenantId = requireTenantId()
            val days = route.days.coerceIn(1, 365)
            val history = bankingService.getBalanceHistory(tenantId, days)
                .getOrElse { throw DokusException.InternalError("Failed to get balance history: ${it.message}") }
            call.respond(HttpStatusCode.OK, history)
        }

        // GET /api/v1/banking/transactions
        get<Banking.Transactions> { route ->
            val tenantId = requireTenantId()

            if (route.limit !in 1..MAX_PAGE_SIZE) {
                throw DokusException.BadRequest("Limit must be between 1 and $MAX_PAGE_SIZE")
            }
            if (route.offset < 0) {
                throw DokusException.BadRequest("Offset must be non-negative")
            }

            val page = bankingService.listTransactions(
                tenantId = tenantId,
                status = route.status,
                source = route.source,
                fromDate = route.fromDate,
                toDate = route.toDate,
                limit = route.limit,
                offset = route.offset.toLong(),
            ).getOrElse { throw DokusException.InternalError("Failed to list transactions: ${it.message}") }

            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse(
                    items = page.items,
                    total = page.total,
                    limit = route.limit,
                    offset = route.offset
                )
            )
        }

        // GET /api/v1/banking/transactions/summary
        get<Banking.Transactions.Summary> {
            val tenantId = requireTenantId()
            val summary = bankingService.getTransactionSummary(tenantId)
                .getOrElse { throw DokusException.InternalError("Failed to get transaction summary: ${it.message}") }
            call.respond(HttpStatusCode.OK, summary)
        }

        // GET /api/v1/banking/transactions/{id}
        get<Banking.Transactions.Id> { route ->
            val tenantId = requireTenantId()
            val transactionId = parseTransactionId(route.id)

            val transaction = bankingService.getTransaction(tenantId, transactionId)
                .getOrElse { error ->
                    throw (error as? DokusException
                        ?: DokusException.InternalError("Failed to get transaction: ${error.message}"))
                }
            call.respond(HttpStatusCode.OK, transaction)
        }

        // POST /api/v1/banking/transactions/{id}/link
        post<Banking.Transactions.Id.Link> { route ->
            val tenantId = requireTenantId()
            val transactionId = parseTransactionId(route.parent.id)
            val request = call.receive<LinkTransactionRequest>()

            val updated = bankingService.linkTransaction(tenantId, transactionId, request.cashflowEntryId)
                .getOrElse { error ->
                    throw (error as? DokusException
                        ?: DokusException.InternalError("Failed to link transaction: ${error.message}"))
                }
            call.respond(HttpStatusCode.OK, updated)
        }

        // POST /api/v1/banking/transactions/{id}/ignore
        post<Banking.Transactions.Id.Ignore> { route ->
            val tenantId = requireTenantId()
            val transactionId = parseTransactionId(route.parent.id)
            val request = call.receive<IgnoreTransactionRequest>()

            val updated = bankingService.ignoreTransaction(
                tenantId = tenantId,
                transactionId = transactionId,
                reason = request.reason,
                ignoredBy = "user", // TODO: extract from principal
            ).getOrElse { error ->
                    throw (error as? DokusException
                        ?: DokusException.InternalError("Failed to ignore transaction: ${error.message}"))
                }
            call.respond(HttpStatusCode.OK, updated)
        }

        // POST /api/v1/banking/transactions/{id}/confirm
        post<Banking.Transactions.Id.Confirm> { route ->
            val tenantId = requireTenantId()
            val transactionId = parseTransactionId(route.parent.id)

            val updated = bankingService.confirmSuggestedMatch(tenantId, transactionId)
                .getOrElse { error ->
                    throw (error as? DokusException
                        ?: DokusException.InternalError("Failed to confirm match: ${error.message}"))
                }
            call.respond(HttpStatusCode.OK, updated)
        }

        // POST /api/v1/banking/transactions/{id}/create-expense
        post<Banking.Transactions.Id.CreateExpense> { route ->
            val tenantId = requireTenantId()
            val transactionId = parseTransactionId(route.parent.id)

            val updated = bankingService.createExpenseFromTransaction(tenantId, transactionId)
                .getOrElse { error ->
                    throw (error as? DokusException
                        ?: DokusException.InternalError("Failed to create expense: ${error.message}"))
                }
            call.respond(HttpStatusCode.OK, updated)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
private fun parseTransactionId(id: String): BankTransactionId = try {
    BankTransactionId(Uuid.parse(id))
} catch (_: Exception) {
    throw DokusException.BadRequest("Invalid transaction ID format")
}
