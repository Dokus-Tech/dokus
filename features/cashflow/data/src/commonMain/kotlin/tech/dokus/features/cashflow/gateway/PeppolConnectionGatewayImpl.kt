package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.Address
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolConnectionGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolConnectionGateway {
    override suspend fun connectPeppol(
        companyAddress: Address
    ) = cashflowRemoteDataSource.connectPeppol(companyAddress)

    override suspend fun getPeppolSettings() = cashflowRemoteDataSource.getPeppolSettings()
}
