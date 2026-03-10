package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.model.DocumentCountsResponse

/**
 * Use case for loading global document badge counts for the inbox screen.
 */
interface GetDocumentCountsUseCase {
    suspend operator fun invoke(): Result<DocumentCountsResponse>
}
