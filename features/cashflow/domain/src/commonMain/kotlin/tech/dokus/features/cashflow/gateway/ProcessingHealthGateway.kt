package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.BulkReprocessRequest
import tech.dokus.domain.model.BulkReprocessResponse
import tech.dokus.domain.model.ProcessingHealthRecommendation

/**
 * Gateway for workspace-level processing health operations.
 */
interface ProcessingHealthGateway {
    suspend fun getRecommendation(): Result<ProcessingHealthRecommendation>
    suspend fun bulkReprocess(request: BulkReprocessRequest = BulkReprocessRequest()): Result<BulkReprocessResponse>
}
