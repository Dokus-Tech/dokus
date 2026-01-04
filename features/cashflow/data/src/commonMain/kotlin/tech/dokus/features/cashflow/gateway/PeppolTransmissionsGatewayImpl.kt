package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolTransmissionsGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolTransmissionsGateway {
    override suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ) = cashflowRemoteDataSource.listPeppolTransmissions(
        direction = direction,
        status = status,
        limit = limit,
        offset = offset
    )
}
