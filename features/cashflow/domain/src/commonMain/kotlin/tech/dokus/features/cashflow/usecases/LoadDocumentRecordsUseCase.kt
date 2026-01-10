package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.enums.DraftStatus
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
        draftStatus: DraftStatus? = null,
        ingestionStatus: IngestionStatus? = null,
        search: String? = null
    ): Result<PaginatedResponse<DocumentRecordDto>>
}
