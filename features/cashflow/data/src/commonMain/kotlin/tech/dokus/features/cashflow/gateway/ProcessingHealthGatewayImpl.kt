package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.BulkReprocessRequest
import tech.dokus.domain.model.BulkReprocessResponse
import tech.dokus.domain.model.ProcessingHealthRecommendation
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class ProcessingHealthGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : ProcessingHealthGateway {
    override suspend fun getRecommendation(): Result<ProcessingHealthRecommendation> =
        cashflowRemoteDataSource.getProcessingHealth()

    override suspend fun bulkReprocess(request: BulkReprocessRequest): Result<BulkReprocessResponse> =
        cashflowRemoteDataSource.bulkReprocess(request)
}
