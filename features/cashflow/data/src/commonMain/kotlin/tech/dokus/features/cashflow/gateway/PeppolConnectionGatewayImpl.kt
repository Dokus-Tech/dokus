package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolConnectionGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolConnectionGateway {
    override suspend fun connectPeppol(
        request: PeppolConnectRequest
    ) = cashflowRemoteDataSource.connectPeppol(request)

    override suspend fun getPeppolSettings() = cashflowRemoteDataSource.getPeppolSettings()
}
