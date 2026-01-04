package tech.dokus.features.cashflow.gateway

import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolInboxGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolInboxGateway {
    override suspend fun pollPeppolInbox() = cashflowRemoteDataSource.pollPeppolInbox()
}
