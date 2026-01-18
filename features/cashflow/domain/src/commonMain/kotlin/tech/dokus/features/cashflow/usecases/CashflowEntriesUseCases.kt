package tech.dokus.features.cashflow.usecases

import kotlinx.datetime.LocalDate
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CancelEntryRequest
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowOverview
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse

/**
 * Use case for loading cashflow overview with filters.
 *
 * @param viewMode Determines date field filtering:
 *                 - Upcoming: filter by eventDate
 *                 - History: filter by paidAt
 * @param statuses Multi-status filter (e.g., [Open, Overdue])
 */
interface GetCashflowOverviewUseCase {
    suspend operator fun invoke(
        viewMode: CashflowViewMode,
        fromDate: LocalDate,
        toDate: LocalDate,
        direction: CashflowDirection? = null,
        statuses: List<CashflowEntryStatus>? = null
    ): Result<CashflowOverview>
}

/**
 * Use case for listing cashflow entries with pagination and filtering.
 *
 * @param viewMode Determines date field filtering:
 *                 - Upcoming: filter by eventDate, sort ASC
 *                 - History: filter by paidAt, sort DESC
 * @param statuses Multi-status filter (e.g., [Open, Overdue])
 */
interface LoadCashflowEntriesUseCase {
    suspend operator fun invoke(
        page: Int,
        pageSize: Int,
        viewMode: CashflowViewMode? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        statuses: List<CashflowEntryStatus>? = null,
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
