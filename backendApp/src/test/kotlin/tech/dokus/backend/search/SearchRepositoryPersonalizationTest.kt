package tech.dokus.backend.search

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchResultEntityType
import tech.dokus.domain.model.SearchSignalEventRequest
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.domain.model.UnifiedSearchScope
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class SearchRepositoryPersonalizationTest : SearchTestFixture() {

    @Test
    fun `blank query returns empty suggestions when no personalization data exists`() = runBlocking {
        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = null,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        assertTrue(response.suggestions.isEmpty())
    }

    @Test
    fun `blank query suggestions are backend personalized and no generic fallback is emitted`() = runBlocking {
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "kbc bank",
            displayText = "KBC Bank",
        ).getOrThrow()

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = null,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        assertTrue(response.suggestions.any { it.label == "KBC Bank" })
        assertFalse(response.suggestions.any { it.label.equals("this month", ignoreCase = true) })
        assertFalse(response.suggestions.any { it.label.equals("documents", ignoreCase = true) })
        assertFalse(response.suggestions.any { it.label.equals("contacts", ignoreCase = true) })
    }

    @Test
    fun `bare overdue and paid tokens are never returned as suggestions`() = runBlocking {
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "overdue",
            displayText = "overdue",
        ).getOrThrow()
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.SuggestionSelected,
            normalizedText = "paid",
            displayText = "paid",
        ).getOrThrow()
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "kbc",
            displayText = "KBC",
        ).getOrThrow()

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = null,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        val labels = response.suggestions.map { it.label.lowercase() }
        assertFalse("overdue" in labels)
        assertFalse("paid" in labels)
        assertTrue(labels.any { it == "kbc" })
    }

    @Test
    fun `mixed signal ranking prefers stronger user events over tenant entity frequency`() = runBlocking {
        repeat(5) {
            insertContact(
                tenant = tenantUuid,
                name = "Beta Corp",
                isActive = true,
            )
        }
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.ResultOpened,
            normalizedText = "alpha corp",
            displayText = "Alpha Corp",
        ).getOrThrow()

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = null,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        assertEquals("Alpha Corp", response.suggestions.firstOrNull()?.label)
        assertTrue(response.suggestions.any { it.label == "Beta Corp" })
    }

    @Test
    fun `signal suggestions are isolated by tenant and user`() = runBlocking {
        val userTwoUuid = UUID.randomUUID()
        val tenantTwoUuid = UUID.randomUUID()
        val userTwoId = UserId(userTwoUuid.toKotlinUuid())
        val tenantTwoId = TenantId(tenantTwoUuid.toKotlinUuid())

        insertUser(userTwoUuid, "other-user@tenant-a.test")
        insertTenant(tenantTwoUuid, "Tenant B")

        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "tenant-a-owner",
            displayText = "Tenant A Owner",
        ).getOrThrow()
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userTwoId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "tenant-a-other",
            displayText = "Tenant A Other User",
        ).getOrThrow()
        searchSignalRepository.upsertSignal(
            tenantId = tenantTwoId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "tenant-b-owner",
            displayText = "Tenant B Owner",
        ).getOrThrow()

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = null,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        val labels = response.suggestions.map { it.label }
        assertTrue("Tenant A Owner" in labels)
        assertFalse("Tenant A Other User" in labels)
        assertFalse("Tenant B Owner" in labels)
    }

    @Test
    fun `overdue preset returns only past open or overdue transactions and forces transactions scope`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val contactId = insertContact(tenantUuid, "Preset Contact")

        val overdueOpen = insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Open,
            eventDate = today.plus(DatePeriod(days = -3)),
            counterpartyId = contactId,
        )
        val overdueFlagged = insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Overdue,
            eventDate = today.plus(DatePeriod(days = -1)),
            counterpartyId = contactId,
        )
        insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Open,
            eventDate = today.plus(DatePeriod(days = 10)),
            counterpartyId = contactId,
        )
        insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Paid,
            eventDate = today.plus(DatePeriod(days = -5)),
            counterpartyId = contactId,
        )

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = SearchPreset.OverdueInvoices,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        val ids = response.transactions.map { it.entryId.toString() }.toSet()
        assertEquals(UnifiedSearchScope.Transactions, response.scope)
        assertEquals(0L, response.counts.documents)
        assertEquals(0L, response.counts.contacts)
        assertEquals(response.counts.transactions, response.counts.all)
        assertTrue(overdueOpen.toString() in ids)
        assertTrue(overdueFlagged.toString() in ids)
        assertTrue(response.transactions.all { it.date < today })
        assertTrue(response.transactions.all { it.status == CashflowEntryStatus.Open || it.status == CashflowEntryStatus.Overdue })
        assertTrue(response.suggestions.isEmpty())
    }

    @Test
    fun `upcoming preset returns due-soon open or overdue transactions with forced scope and counts`() = runBlocking {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val contactId = insertContact(tenantUuid, "Preset Contact")

        val dueSoonOpen = insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Open,
            eventDate = today.plus(DatePeriod(days = 5)),
            counterpartyId = contactId,
        )
        val dueSoonOverdue = insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Overdue,
            eventDate = today.plus(DatePeriod(days = 30)),
            counterpartyId = contactId,
        )
        insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Open,
            eventDate = today.plus(DatePeriod(days = 35)),
            counterpartyId = contactId,
        )
        insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Overdue,
            eventDate = today.plus(DatePeriod(days = -2)),
            counterpartyId = contactId,
        )

        val response = searchRepository.search(
            tenantId = tenantId,
            userId = userId,
            query = "",
            scope = UnifiedSearchScope.All,
            preset = SearchPreset.UpcomingPayments,
            limit = 20,
            suggestionLimit = 8,
        ).getOrThrow()

        val ids = response.transactions.map { it.entryId.toString() }.toSet()
        assertEquals(UnifiedSearchScope.Transactions, response.scope)
        assertEquals(response.counts.transactions, response.counts.all)
        assertTrue(dueSoonOpen.toString() in ids)
        assertTrue(dueSoonOverdue.toString() in ids)
        assertTrue(response.transactions.all { it.status == CashflowEntryStatus.Open || it.status == CashflowEntryStatus.Overdue })
        assertTrue(response.transactions.all { it.date >= today && it.date <= today.plus(DatePeriod(days = 30)) })
    }

    @Test
    fun `signal upsert increments count without duplicate rows and resolves labels for entities`() = runBlocking {
        val contactUuid = insertContact(tenantUuid, "Label Contact")
        val documentUuid = insertDocument(tenantUuid, "label-document.pdf")
        val transactionUuid = insertCashflowEntry(
            tenant = tenantUuid,
            status = CashflowEntryStatus.Open,
            eventDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date,
            counterpartyId = contactUuid,
        )

        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "label test",
            displayText = "Label Test",
        ).getOrThrow()
        searchSignalRepository.upsertSignal(
            tenantId = tenantId,
            userId = userId,
            signalType = SearchSignalEventType.QueryCommitted,
            normalizedText = "label test",
            displayText = "Label Test",
        ).getOrThrow()

        val signals = searchSignalRepository.topUserSignals(tenantId, userId, 10).getOrThrow()
        val row = signals.single { it.normalizedText == "label test" }
        assertEquals(2L, row.count)

        assertEquals(
            "label-document.pdf",
            searchSignalRepository.resolveEntityLabel(
                tenantId = tenantId,
                entityType = SearchResultEntityType.Document,
                entityId = documentUuid.toString(),
            ).getOrThrow()
        )
        assertEquals(
            "Label Contact",
            searchSignalRepository.resolveEntityLabel(
                tenantId = tenantId,
                entityType = SearchResultEntityType.Contact,
                entityId = contactUuid.toString(),
            ).getOrThrow()
        )
        assertNotNull(
            searchSignalRepository.resolveEntityLabel(
                tenantId = tenantId,
                entityType = SearchResultEntityType.Transaction,
                entityId = transactionUuid.toString(),
            ).getOrThrow()
        )
    }

    @Test
    fun `invalid result-open entity id is ignored safely`() = runBlocking {
        val result = searchService.recordSignal(
            tenantId = tenantId,
            userId = userId,
            request = SearchSignalEventRequest(
                eventType = SearchSignalEventType.ResultOpened,
                query = null,
                resultEntityType = SearchResultEntityType.Document,
                resultEntityId = "not-a-uuid",
            )
        )

        assertTrue(result.isSuccess)
        val signals = searchSignalRepository.topUserSignals(tenantId, userId, 10).getOrThrow()
        assertTrue(signals.isEmpty())
    }
}
