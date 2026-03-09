package tech.dokus.features.cashflow.presentation.ledger.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode as DomainCashflowViewMode
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashInSummary
import tech.dokus.domain.model.CashOutSummary
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowOverview
import tech.dokus.domain.model.CashflowPeriod
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.usecases.GetCashflowOverviewUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowEntriesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CashflowLedgerContainerRefreshTest {

    @Test
    fun `set view mode keeps content visible and swaps summary plus entries atomically`() = runTest {
        val upcomingEntries = listOf(
            cashflowEntry("00000000-0000-0000-0000-000000000101", CashflowDirection.In, 12000L),
            cashflowEntry("00000000-0000-0000-0000-000000000102", CashflowDirection.Out, -4000L),
        )
        val overdueEntries = listOf(
            cashflowEntry("00000000-0000-0000-0000-000000000201", CashflowDirection.Out, -2500L),
        )

        val overviewUseCase = FakeGetCashflowOverviewUseCase().apply {
            enqueueResult(Result.success(cashflowOverview(net = 8000L, cashIn = 12000L, cashOut = 4000L)))
        }
        val entriesUseCase = FakeLoadCashflowEntriesUseCase().apply {
            enqueueResult(Result.success(pageResponse(upcomingEntries)))
        }
        val paymentUseCase = FakeRecordCashflowPaymentUseCase()

        val deferredOverview = CompletableDeferred<Result<CashflowOverview>>()
        val deferredEntries = CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>()
        overviewUseCase.enqueueDeferred(deferredOverview)
        entriesUseCase.enqueueDeferred(deferredEntries)

        val container = CashflowLedgerContainer(
            loadCashflowEntries = entriesUseCase,
            getCashflowOverview = overviewUseCase,
            recordPayment = paymentUseCase,
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertTrue(initial.entries.isSuccess())
            val initialEntryIds = initial.entries.lastData?.data?.map { it.id }
            val initialSummary = initial.summary

            emit(CashflowLedgerIntent.SetViewMode(CashflowViewMode.Overdue))
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.entries.isLoading())
            assertEquals(CashflowViewMode.Overdue, refreshing.filters.viewMode)
            assertEquals(initialEntryIds, refreshing.entries.lastData?.data?.map { it.id })
            assertEquals(initialSummary, refreshing.summary)

            deferredOverview.complete(Result.success(cashflowOverview(net = -2500L, cashIn = 0L, cashOut = 2500L)))
            runCurrent()

            val stillRefreshing = states.value
            assertTrue(stillRefreshing.entries.isLoading())
            assertEquals(initialEntryIds, stillRefreshing.entries.lastData?.data?.map { it.id })
            assertEquals(initialSummary, stillRefreshing.summary)

            deferredEntries.complete(Result.success(pageResponse(overdueEntries)))
            advanceUntilIdle()

            val updated = states.value
            assertFalse(updated.entries.isLoading())
            assertEquals(CashflowViewMode.Overdue, updated.filters.viewMode)
            assertEquals(overdueEntries.map { it.id }, updated.entries.lastData?.data?.map { it.id })
            assertEquals(Money(-2500L), updated.summary.netAmount)
            assertEquals(Money(0L), updated.summary.totalIn)
            assertEquals(Money(2500L), updated.summary.totalOut)
        }
    }

    @Test
    fun `set direction filter keeps content visible and refreshes in place`() = runTest {
        val initialEntries = listOf(
            cashflowEntry("00000000-0000-0000-0000-000000000301", CashflowDirection.In, 9000L),
            cashflowEntry("00000000-0000-0000-0000-000000000302", CashflowDirection.Out, -3000L),
        )
        val outEntries = listOf(
            cashflowEntry("00000000-0000-0000-0000-000000000401", CashflowDirection.Out, -3000L),
        )

        val overviewUseCase = FakeGetCashflowOverviewUseCase().apply {
            enqueueResult(Result.success(cashflowOverview(net = 6000L, cashIn = 9000L, cashOut = 3000L)))
        }
        val entriesUseCase = FakeLoadCashflowEntriesUseCase().apply {
            enqueueResult(Result.success(pageResponse(initialEntries)))
        }

        val deferredOverview = CompletableDeferred<Result<CashflowOverview>>()
        val deferredEntries = CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>()
        overviewUseCase.enqueueDeferred(deferredOverview)
        entriesUseCase.enqueueDeferred(deferredEntries)

        val container = CashflowLedgerContainer(
            loadCashflowEntries = entriesUseCase,
            getCashflowOverview = overviewUseCase,
            recordPayment = FakeRecordCashflowPaymentUseCase(),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            val initial = states.value
            val initialEntryIds = initial.entries.lastData?.data?.map { it.id }

            emit(CashflowLedgerIntent.SetDirectionFilter(DirectionFilter.Out))
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.entries.isLoading())
            assertEquals(DirectionFilter.Out, refreshing.filters.direction)
            assertEquals(initialEntryIds, refreshing.entries.lastData?.data?.map { it.id })

            deferredOverview.complete(Result.success(cashflowOverview(net = -3000L, cashIn = 0L, cashOut = 3000L)))
            deferredEntries.complete(Result.success(pageResponse(outEntries)))
            advanceUntilIdle()

            val updated = states.value
            assertFalse(updated.entries.isLoading())
            assertEquals(DirectionFilter.Out, updated.filters.direction)
            assertEquals(outEntries.map { it.id }, updated.entries.lastData?.data?.map { it.id })
        }
    }

    @Test
    fun `view mode change error preserves data and keeps new filter`() = runTest {
        val upcomingEntries = listOf(
            cashflowEntry("00000000-0000-0000-0000-000000000501", CashflowDirection.In, 5000L),
        )

        val overviewUseCase = FakeGetCashflowOverviewUseCase().apply {
            enqueueResult(Result.success(cashflowOverview(net = 5000L, cashIn = 5000L, cashOut = 0L)))
        }
        val entriesUseCase = FakeLoadCashflowEntriesUseCase().apply {
            enqueueResult(Result.success(pageResponse(upcomingEntries)))
        }

        // Enqueue failing results for the Overdue switch
        val deferredOverview = CompletableDeferred<Result<CashflowOverview>>()
        val deferredEntries = CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>()
        overviewUseCase.enqueueDeferred(deferredOverview)
        entriesUseCase.enqueueDeferred(deferredEntries)

        val container = CashflowLedgerContainer(
            loadCashflowEntries = entriesUseCase,
            getCashflowOverview = overviewUseCase,
            recordPayment = FakeRecordCashflowPaymentUseCase(),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertEquals(CashflowViewMode.Upcoming, initial.filters.viewMode)
            assertEquals(upcomingEntries.map { it.id }, initial.entries.lastData?.data?.map { it.id })

            emit(CashflowLedgerIntent.SetViewMode(CashflowViewMode.Overdue))
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.entries.isLoading())
            assertEquals(CashflowViewMode.Overdue, refreshing.filters.viewMode)

            deferredOverview.complete(Result.failure(RuntimeException("network error")))
            deferredEntries.complete(Result.failure(RuntimeException("network error")))
            advanceUntilIdle()

            val errorState = states.value
            assertTrue(errorState.entries.isError())
            assertEquals(CashflowViewMode.Overdue, errorState.filters.viewMode)
            assertEquals(upcomingEntries.map { it.id }, errorState.entries.lastData?.data?.map { it.id })
        }
    }
}

private class FakeGetCashflowOverviewUseCase : GetCashflowOverviewUseCase {
    private val results = ArrayDeque<CompletableDeferred<Result<CashflowOverview>>>()

    fun enqueueResult(result: Result<CashflowOverview>) {
        enqueueDeferred(
            CompletableDeferred<Result<CashflowOverview>>().apply { complete(result) }
        )
    }

    fun enqueueDeferred(deferred: CompletableDeferred<Result<CashflowOverview>>) {
        results.addLast(deferred)
    }

    override suspend fun invoke(
        viewMode: DomainCashflowViewMode,
        fromDate: LocalDate,
        toDate: LocalDate,
        direction: CashflowDirection?,
        statuses: List<CashflowEntryStatus>?,
    ): Result<CashflowOverview> {
        return requireNotNull(results.removeFirstOrNull()) {
            "No overview result queued"
        }.await()
    }
}

private class FakeLoadCashflowEntriesUseCase : LoadCashflowEntriesUseCase {
    private val results = ArrayDeque<CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>>()

    fun enqueueResult(result: Result<PaginatedResponse<CashflowEntry>>) {
        enqueueDeferred(
            CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>().apply { complete(result) }
        )
    }

    fun enqueueDeferred(deferred: CompletableDeferred<Result<PaginatedResponse<CashflowEntry>>>) {
        results.addLast(deferred)
    }

    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        viewMode: DomainCashflowViewMode?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        direction: CashflowDirection?,
        statuses: List<CashflowEntryStatus>?,
        sourceType: CashflowSourceType?,
        entryId: CashflowEntryId?,
    ): Result<PaginatedResponse<CashflowEntry>> {
        return requireNotNull(results.removeFirstOrNull()) {
            "No entries result queued"
        }.await()
    }
}

private class FakeRecordCashflowPaymentUseCase : RecordCashflowPaymentUseCase {
    override suspend fun invoke(
        entryId: CashflowEntryId,
        request: CashflowPaymentRequest,
    ): Result<CashflowEntry> {
        return Result.failure(IllegalStateException("Not used in refresh tests"))
    }
}

private fun pageResponse(items: List<CashflowEntry>): PaginatedResponse<CashflowEntry> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 50,
        offset = 0,
        hasMore = false
    )
}

private fun cashflowOverview(net: Long, cashIn: Long, cashOut: Long): CashflowOverview {
    return CashflowOverview(
        period = CashflowPeriod(
            from = LocalDate(2026, 1, 1),
            to = LocalDate(2026, 1, 31)
        ),
        cashIn = CashInSummary(
            total = Money(cashIn),
            paid = Money.ZERO,
            pending = Money.ZERO,
            overdue = Money.ZERO,
            invoiceCount = 0
        ),
        cashOut = CashOutSummary(
            total = Money(cashOut),
            paid = Money.ZERO,
            pending = Money.ZERO,
            expenseCount = 0,
            inboundInvoiceCount = 0
        ),
        netCashflow = Money(net),
        currency = Currency.Eur
    )
}

private fun cashflowEntry(
    id: String,
    direction: CashflowDirection,
    amountMinor: Long,
): CashflowEntry {
    val amount = Money(amountMinor)
    return CashflowEntry(
        id = CashflowEntryId.parse(id),
        tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001"),
        sourceType = CashflowSourceType.Invoice,
        sourceId = "source-$id",
        documentId = null,
        direction = direction,
        eventDate = LocalDate(2026, 1, 10),
        amountGross = amount,
        amountVat = Money.ZERO,
        remainingAmount = Money(kotlin.math.abs(amountMinor)),
        currency = Currency.Eur,
        status = CashflowEntryStatus.Open,
        paidAt = null,
        contactId = null,
        contactName = null,
        description = "entry-$id",
        createdAt = LocalDateTime(2026, 1, 1, 10, 0),
        updatedAt = LocalDateTime(2026, 1, 1, 10, 0),
    )
}
