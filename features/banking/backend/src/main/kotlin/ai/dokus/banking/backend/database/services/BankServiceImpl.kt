@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.banking.backend.database.services

import ai.dokus.foundation.domain.ids.BankConnectionId
import ai.dokus.foundation.domain.ids.BankTransactionId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.model.BankConnectionDto
import ai.dokus.foundation.domain.model.BankTransactionDto
import ai.dokus.foundation.ktor.services.BankService
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class BankServiceImpl : BankService {

    override suspend fun connectBank(
        organizationId: OrganizationId,
        provider: BankProvider,
        institutionId: String,
        institutionName: String,
        accountId: String,
        accountName: String?,
        accessToken: String
    ): BankConnectionDto {
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

    override suspend fun syncAllConnections(organizationId: OrganizationId): Map<BankConnectionId, Int> {
        TODO("Not yet implemented")
    }

    override suspend fun listConnections(organizationId: OrganizationId, activeOnly: Boolean): List<BankConnectionDto> {
        TODO("Not yet implemented")
    }

    override suspend fun findConnectionById(id: BankConnectionId): BankConnectionDto? {
        TODO("Not yet implemented")
    }

    override suspend fun listTransactions(
        organizationId: OrganizationId,
        connectionId: BankConnectionId?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        reconciled: Boolean?,
        limit: Int?,
        offset: Int?
    ): List<BankTransactionDto> {
        TODO("Not yet implemented")
    }

    override suspend fun findTransactionById(id: BankTransactionId): BankTransactionDto? {
        TODO("Not yet implemented")
    }

    override suspend fun listUnreconciled(organizationId: OrganizationId): List<BankTransactionDto> {
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

    override suspend fun autoReconcile(organizationId: OrganizationId): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getLastSyncTime(connectionId: BankConnectionId): Instant? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteConnection(connectionId: BankConnectionId) {
        TODO("Not yet implemented")
    }
}
