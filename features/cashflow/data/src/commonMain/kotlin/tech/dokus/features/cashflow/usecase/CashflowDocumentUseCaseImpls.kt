package tech.dokus.features.cashflow.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.SubmitInvoiceUseCase
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState

private const val DefaultDocumentLimit = 100

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
                        DocumentStatus.NeedsReview,
                        DocumentStatus.Ready
                    )
                    val collected = mutableListOf<DocumentRecordDto>()
                    for (status in statuses) {
                        val result = cashflowRemoteDataSource.listDocuments(
                            documentStatus = status,
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
