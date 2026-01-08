package tech.dokus.features.cashflow.usecases

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse

/**
 * Use case for listing cashflow entries with pagination and filtering.
 */
interface LoadCashflowEntriesUseCase {
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        status: CashflowEntryStatus? = null,
        sourceType: CashflowSourceType? = null,
        entryId: CashflowEntryId? = null
    ): Result<PaginatedResponse<CashflowEntry>>
}

/**
 * Use case for getting a single cashflow entry by ID.
 */
interface GetCashflowEntryUseCase {
    suspend operator fun invoke(entryId: CashflowEntryId): Result<CashflowEntry>
}

/**
 * Use case for recording a payment against a cashflow entry.
 */
interface RecordCashflowPaymentUseCase {
    suspend operator fun invoke(
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest
    ): Result<CashflowEntry>
}

/**
 * Use case for cancelling a cashflow entry.
 */
interface CancelCashflowEntryUseCase {
    suspend operator fun invoke(
        entryId: CashflowEntryId,
        request: CancelEntryRequest? = null
    ): Result<CashflowEntry>
}
