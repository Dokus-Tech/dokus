package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.model.BulkReprocessResponse
import tech.dokus.domain.model.ProcessingHealthRecommendation
import tech.dokus.features.cashflow.gateway.ProcessingHealthGateway
import tech.dokus.features.cashflow.usecases.ExecuteBulkReprocessUseCase
import tech.dokus.features.cashflow.usecases.GetProcessingHealthUseCase

internal class GetProcessingHealthUseCaseImpl(
    private val gateway: ProcessingHealthGateway
) : GetProcessingHealthUseCase {
    override suspend fun invoke(): Result<ProcessingHealthRecommendation> =
        gateway.getRecommendation()
}

internal class ExecuteBulkReprocessUseCaseImpl(
    private val gateway: ProcessingHealthGateway
) : ExecuteBulkReprocessUseCase {
    override suspend fun invoke(): Result<BulkReprocessResponse> =
        gateway.bulkReprocess()
}
