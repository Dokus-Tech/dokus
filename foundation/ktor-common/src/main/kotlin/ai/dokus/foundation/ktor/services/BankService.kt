package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.BankConnectionId
import ai.dokus.foundation.domain.ids.BankTransactionId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.model.BankConnection
import ai.dokus.foundation.domain.model.BankTransaction
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc
import kotlin.time.ExperimentalTime

@Rpc
interface BankService {
    /**
     * Connects a bank account via Plaid/Tink/Nordigen
     * Stores encrypted access token for transaction sync
     *
     * @param organizationId The tenant's unique identifier
     * @param provider The bank provider (Plaid, Tink, Nordigen)
     * @param institutionId The institution ID from the provider
     * @param institutionName The institution name
     * @param accountId The account ID from the provider
     * @param accountName The account name (optional)
     * @param accessToken The access token (will be encrypted)
     * @return The created bank connection
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun connectBank(
        organizationId: OrganizationId,
        provider: BankProvider,
        institutionId: String,
        institutionName: String,
        accountId: String,
        accountName: String? = null,
        accessToken: String
    ): BankConnection

    /**
     * Disconnects a bank account
     * Marks the connection as inactive
     *
     * @param connectionId The bank connection's unique identifier
     * @throws IllegalArgumentException if connection not found
     */
    suspend fun disconnect(connectionId: BankConnectionId)

    /**
     * Reactivates a previously disconnected bank connection
     *
     * @param connectionId The bank connection's unique identifier
     * @param accessToken The new access token (will be encrypted)
     * @throws IllegalArgumentException if connection not found
     */
    suspend fun reconnect(connectionId: BankConnectionId, accessToken: String)

    /**
     * Updates the access token for a bank connection
     * Used when tokens are refreshed by the provider
     *
     * @param connectionId The bank connection's unique identifier
     * @param accessToken The new access token (will be encrypted)
     * @throws IllegalArgumentException if connection not found
     */
    suspend fun updateAccessToken(connectionId: BankConnectionId, accessToken: String)

    /**
     * Syncs transactions from a bank connection
     * Imports new transactions and updates existing ones
     *
     * @param connectionId The bank connection's unique identifier
     * @param fromDate Import transactions from this date (optional)
     * @return Number of transactions imported
     * @throws IllegalArgumentException if connection not found or sync fails
     */
    suspend fun syncTransactions(connectionId: BankConnectionId, fromDate: LocalDate? = null): Int

    /**
     * Syncs transactions for all active bank connections of a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @return Map of connection ID to number of transactions imported
     */
    suspend fun syncAllConnections(organizationId: OrganizationId): Map<BankConnectionId, Int>

    /**
     * Lists all bank connections for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @param activeOnly If true, only returns active connections (defaults to true)
     * @return List of bank connections
     */
    suspend fun listConnections(organizationId: OrganizationId, activeOnly: Boolean = true): List<BankConnection>

    /**
     * Finds a bank connection by its unique ID
     *
     * @param id The connection's unique identifier
     * @return The bank connection if found, null otherwise
     */
    suspend fun findConnectionById(id: BankConnectionId): BankConnection?

    /**
     * Lists bank transactions for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @param connectionId Filter by specific connection (optional)
     * @param fromDate Filter transactions on or after this date (optional)
     * @param toDate Filter transactions on or before this date (optional)
     * @param reconciled Filter by reconciliation status (optional, null = all)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of bank transactions
     */
    suspend fun listTransactions(
        organizationId: OrganizationId,
        connectionId: BankConnectionId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        reconciled: Boolean? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<BankTransaction>

    /**
     * Finds a bank transaction by its unique ID
     *
     * @param id The transaction's unique identifier
     * @return The bank transaction if found, null otherwise
     */
    suspend fun findTransactionById(id: BankTransactionId): BankTransaction?

    /**
     * Lists unreconciled transactions for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @return List of unreconciled transactions
     */
    suspend fun listUnreconciled(organizationId: OrganizationId): List<BankTransaction>

    /**
     * Reconciles a bank transaction with an expense
     *
     * @param transactionId The transaction's unique identifier
     * @param expenseId The expense's unique identifier
     * @throws IllegalArgumentException if transaction or expense not found
     */
    suspend fun reconcileWithExpense(transactionId: BankTransactionId, expenseId: ExpenseId)

    /**
     * Reconciles a bank transaction with an invoice payment
     *
     * @param transactionId The transaction's unique identifier
     * @param invoiceId The invoice's unique identifier
     * @throws IllegalArgumentException if transaction or invoice not found
     */
    suspend fun reconcileWithInvoice(transactionId: BankTransactionId, invoiceId: InvoiceId)

    /**
     * Unreconciles a bank transaction
     * Removes the link to expense or invoice
     *
     * @param transactionId The transaction's unique identifier
     * @throws IllegalArgumentException if transaction not found
     */
    suspend fun unreconcile(transactionId: BankTransactionId)

    /**
     * Auto-matches bank transactions with expenses/invoices
     * Uses amount, date, and description to find likely matches
     *
     * @param organizationId The tenant's unique identifier
     * @return Number of transactions auto-reconciled
     */
    suspend fun autoReconcile(organizationId: OrganizationId): Int

    /**
     * Gets the last sync time for a bank connection
     *
     * @param connectionId The bank connection's unique identifier
     * @return The last sync timestamp, or null if never synced
     */
    @OptIn(ExperimentalTime::class)
    suspend fun getLastSyncTime(connectionId: BankConnectionId): Instant?

    /**
     * Deletes a bank connection and all its transactions
     * This is a hard delete - use with caution
     *
     * @param connectionId The bank connection's unique identifier
     * @throws IllegalArgumentException if connection not found
     */
    suspend fun deleteConnection(connectionId: BankConnectionId)
}
