@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.banking.backend.database.services

import ai.dokus.foundation.domain.BankConnectionId
import ai.dokus.foundation.domain.BankTransactionId
import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.model.BankConnection
import ai.dokus.foundation.domain.model.BankTransaction
import ai.dokus.foundation.ktor.services.BankService
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class BankServiceImpl : BankService {

    override suspend fun connectBank(
        tenantId: TenantId,
        provider: BankProvider,
        institutionId: String,
        institutionName: String,
        accountId: String,
        accountName: String?,
        accessToken: String
    ): BankConnection {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect(connectionId: BankConnectionId) {
        TODO("Not yet implemented")
    }

    override suspend fun reconnect(connectionId: BankConnectionId, accessToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun updateAccessToken(connectionId: BankConnectionId, accessToken: String) {
        TODO("Not yet implemented")
    }

    override suspend fun syncTransactions(connectionId: BankConnectionId, fromDate: LocalDate?): Int {
        TODO("Not yet implemented")
    }

    override suspend fun syncAllConnections(tenantId: TenantId): Map<BankConnectionId, Int> {
        TODO("Not yet implemented")
    }

    override suspend fun listConnections(tenantId: TenantId, activeOnly: Boolean): List<BankConnection> {
        TODO("Not yet implemented")
    }

    override suspend fun findConnectionById(id: BankConnectionId): BankConnection? {
        TODO("Not yet implemented")
    }

    override suspend fun listTransactions(
        tenantId: TenantId,
        connectionId: BankConnectionId?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        reconciled: Boolean?,
        limit: Int?,
        offset: Int?
    ): List<BankTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun findTransactionById(id: BankTransactionId): BankTransaction? {
        TODO("Not yet implemented")
    }

    override suspend fun listUnreconciled(tenantId: TenantId): List<BankTransaction> {
        TODO("Not yet implemented")
    }

    override suspend fun reconcileWithExpense(transactionId: BankTransactionId, expenseId: ExpenseId) {
        TODO("Not yet implemented")
    }

    override suspend fun reconcileWithInvoice(transactionId: BankTransactionId, invoiceId: InvoiceId) {
        TODO("Not yet implemented")
    }

    override suspend fun unreconcile(transactionId: BankTransactionId) {
        TODO("Not yet implemented")
    }

    override suspend fun autoReconcile(tenantId: TenantId): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getLastSyncTime(connectionId: BankConnectionId): Instant? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteConnection(connectionId: BankConnectionId) {
        TODO("Not yet implemented")
    }
}
