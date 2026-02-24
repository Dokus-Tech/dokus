package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginatedResponse

/**
 * Use case for loading document records from the operational inbox.
 */
interface LoadDocumentRecordsUseCase {
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        filter: DocumentListFilter? = null,
        documentStatus: DocumentStatus? = null,
        ingestionStatus: IngestionStatus? = null,
    ): Result<PaginatedResponse<DocumentRecordDto>>
}
