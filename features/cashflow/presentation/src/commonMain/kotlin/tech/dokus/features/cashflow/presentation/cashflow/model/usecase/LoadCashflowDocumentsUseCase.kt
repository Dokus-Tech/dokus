package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import kotlinx.datetime.LocalDate

/**
 * Loads cashflow documents from the remote data source with pagination support.
 *
 * This use case handles the data fetching layer for the cashflow screen,
 * abstracting the data source access from the ViewModel.
 */
internal class LoadCashflowDocumentsUseCase(
    private val dataSource: CashflowRemoteDataSource
) {

    /**
     * Load a page of cashflow documents.
     *
     * @param page The page number to load (0-indexed)
     * @param pageSize The number of items per page
     * @param fromDate Optional start date filter
     * @param toDate Optional end date filter
     * @return Result containing paginated financial documents
     */
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<PaginatedResponse<FinancialDocumentDto>> {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }

        val offset = page * pageSize
        return dataSource.listCashflowDocuments(
            fromDate = fromDate,
            toDate = toDate,
            limit = pageSize,
            offset = offset
        )
    }

    /**
     * Load all documents for search purposes.
     *
     * Fetches all available documents page by page until no more remain.
     * This is used when searching across the entire document set.
     *
     * @param pageSize The number of items to fetch per request
     * @param fromDate Optional start date filter
     * @param toDate Optional end date filter
     * @return Result containing all matching financial documents
     */
    suspend fun loadAll(
        pageSize: Int,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<FinancialDocumentDto>> {
        require(pageSize > 0) { "Page size must be positive" }

        val allDocuments = mutableListOf<FinancialDocumentDto>()
        var offset = 0
        var hasMore: Boolean

        do {
            val pageResult = dataSource.listCashflowDocuments(
                fromDate = fromDate,
                toDate = toDate,
                limit = pageSize,
                offset = offset
            )

            if (pageResult.isFailure) {
                return Result.failure(pageResult.exceptionOrNull()!!)
            }

            val page = pageResult.getOrThrow()
            allDocuments.addAll(page.items)
            hasMore = page.hasMore
            offset += pageSize
        } while (hasMore)

        return Result.success(allDocuments)
    }
}
