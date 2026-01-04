package tech.dokus.features.cashflow.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.LoadCashflowDocumentsUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceUseCase
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState

private const val DefaultDocumentLimit = 100

internal class LoadCashflowDocumentsUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : LoadCashflowDocumentsUseCase {
    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<PaginatedResponse<FinancialDocumentDto>> {
        require(page >= 0) { "Page must be non-negative" }
        require(pageSize > 0) { "Page size must be positive" }

        val offset = page * pageSize
        return cashflowRemoteDataSource.listCashflowDocuments(
            fromDate = fromDate,
            toDate = toDate,
            limit = pageSize,
            offset = offset
        )
    }

    override suspend fun loadAll(
        pageSize: Int,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<List<FinancialDocumentDto>> {
        require(pageSize > 0) { "Page size must be positive" }

        val allDocuments = mutableListOf<FinancialDocumentDto>()
        var offset = 0
        var hasMore: Boolean

        do {
            val pageResult = cashflowRemoteDataSource.listCashflowDocuments(
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

internal class WatchPendingDocumentsUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : WatchPendingDocumentsUseCase {
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override operator fun invoke(
        limit: Int
    ): Flow<DokusState<List<DocumentRecordDto>>> {
        require(limit > 0) { "Limit must be positive" }

        return refreshTrigger
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    emit(DokusState.loading())
                    val statuses = listOf(
                        DraftStatus.NeedsReview,
                        DraftStatus.Ready,
                        DraftStatus.NeedsInput
                    )
                    val collected = mutableListOf<DocumentRecordDto>()
                    for (status in statuses) {
                        val result = cashflowRemoteDataSource.listDocuments(
                            draftStatus = status,
                            page = 0,
                            limit = limit.coerceAtMost(DefaultDocumentLimit)
                        )
                        val failed = result.exceptionOrNull()
                        if (failed != null) {
                            emit(DokusState.error(failed.asDokusException) { refresh() })
                            return@flow
                        }
                        collected += result.getOrThrow().items
                    }

                    val unique = collected.distinctBy { it.document.id }
                    emit(DokusState.success(unique))
                }
            }
    }

    override fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }
}

internal class SubmitInvoiceUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : SubmitInvoiceUseCase {
    override suspend fun invoke(
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto> {
        return cashflowRemoteDataSource.createInvoice(request)
    }
}
