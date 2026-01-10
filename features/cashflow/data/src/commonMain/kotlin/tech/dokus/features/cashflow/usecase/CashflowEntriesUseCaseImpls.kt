package tech.dokus.features.cashflow.usecase

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.CancelCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowEntriesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase

private const val PAGE_SIZE_DEFAULT = 50

internal class LoadCashflowEntriesUseCaseImpl(
    private val dataSource: CashflowRemoteDataSource
) : LoadCashflowEntriesUseCase {
    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        direction: CashflowDirection?,
        status: CashflowEntryStatus?,
        sourceType: CashflowSourceType?,
        entryId: CashflowEntryId?
    ): Result<PaginatedResponse<CashflowEntry>> {
        return dataSource.listCashflowEntries(
            fromDate = fromDate,
            toDate = toDate,
            direction = direction,
            status = status,
            sourceType = sourceType,
            entryId = entryId,
            limit = pageSize,
            offset = page * pageSize
        )
    }
}

internal class GetCashflowEntryUseCaseImpl(
    private val dataSource: CashflowRemoteDataSource
) : GetCashflowEntryUseCase {
    override suspend fun invoke(entryId: CashflowEntryId): Result<CashflowEntry> {
        return dataSource.getCashflowEntry(entryId)
    }
}

internal class RecordCashflowPaymentUseCaseImpl(
    private val dataSource: CashflowRemoteDataSource
) : RecordCashflowPaymentUseCase {
    override suspend fun invoke(
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest
    ): Result<CashflowEntry> {
        return dataSource.recordCashflowPayment(entryId, request)
    }
}

internal class CancelCashflowEntryUseCaseImpl(
    private val dataSource: CashflowRemoteDataSource
) : CancelCashflowEntryUseCase {
    override suspend fun invoke(
        entryId: CashflowEntryId,
        request: CancelEntryRequest?
    ): Result<CashflowEntry> {
        return dataSource.cancelCashflowEntry(entryId, request)
    }
}
