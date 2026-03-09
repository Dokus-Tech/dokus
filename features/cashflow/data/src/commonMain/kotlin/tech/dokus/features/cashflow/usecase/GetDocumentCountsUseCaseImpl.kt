package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.GetDocumentCountsUseCase

internal class GetDocumentCountsUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : GetDocumentCountsUseCase {
    override suspend fun invoke(): Result<DocumentCountsResponse> {
        return remoteDataSource.getDocumentCounts()
    }
}
