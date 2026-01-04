package tech.dokus.features.cashflow.gateway

import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolRecipientGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolRecipientGateway {
    override suspend fun verifyPeppolRecipient(
        peppolId: String
    ) = cashflowRemoteDataSource.verifyPeppolRecipient(peppolId)
}
