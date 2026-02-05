package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase

/**
 * Implementation of [LoadDocumentRecordsUseCase] that fetches document records from the API.
 */
internal class LoadDocumentRecordsUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : LoadDocumentRecordsUseCase {

    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        documentStatus: DocumentStatus?,
        ingestionStatus: IngestionStatus?,
        search: String?
    ): Result<PaginatedResponse<DocumentRecordDto>> {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }

        return remoteDataSource.listDocuments(
            documentStatus = documentStatus,
            ingestionStatus = ingestionStatus,
            search = search?.takeIf { it.isNotBlank() },
            page = page,
            limit = pageSize
        )
    }
}
