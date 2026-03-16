package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.model.BulkReprocessResponse
import tech.dokus.domain.model.ProcessingHealthRecommendation

/**
 * Get processing health recommendation for the workspace.
 */
interface GetProcessingHealthUseCase {
    suspend operator fun invoke(): Result<ProcessingHealthRecommendation>
}

/**
 * Execute bulk reprocess of eligible documents.
 */
interface ExecuteBulkReprocessUseCase {
    suspend operator fun invoke(): Result<BulkReprocessResponse>
}
