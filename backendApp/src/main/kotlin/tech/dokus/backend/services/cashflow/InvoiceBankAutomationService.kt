package tech.dokus.backend.services.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.cashflow.matching.MatchingEngine
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor

private const val RECENT_TX_WINDOW_DAYS = 120

/**
 * Trigger facade: routes matching events to the [MatchingEngine].
 *
 * Three entry points:
 * 1. Bank statement imported → match new transactions against open entries
 * 2. Invoice confirmed → match recent transactions against the new entry
 * 3. Contact updated → re-match transactions for the contact's open entries
 */
class InvoiceBankAutomationService(
    private val importedBankTransactionRepository: BankTransactionRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val matchingEngine: MatchingEngine,
    private val tenantRepository: TenantRepository,
) {
    private val logger = loggerFor()

    suspend fun onBankStatementImported(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        val transactions = importedBankTransactionRepository.listByDocument(tenantId, documentId)
        if (transactions.isEmpty()) return

        matchingEngine.matchTransactions(
            tenantId = tenantId,
            transactions = transactions,
            triggerSource = AutoPaymentTriggerSource.BankImport,
        )
    }

    suspend fun onInvoiceConfirmed(
        tenantId: TenantId,
        entryId: CashflowEntryId,
    ) {
        val entry = cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull() ?: return
        if (entry.sourceType != tech.dokus.domain.enums.CashflowSourceType.Invoice) return

        val fromDate = Clock.System.now().toLocalDateTime(
            TimeZone.UTC,
        ).date.minus(DatePeriod(days = RECENT_TX_WINDOW_DAYS))
        val transactions = importedBankTransactionRepository.listRecentCandidatePool(tenantId, fromDate)
        if (transactions.isEmpty()) return

        matchingEngine.matchTransactions(
            tenantId = tenantId,
            transactions = transactions,
            triggerSource = AutoPaymentTriggerSource.InvoiceConfirmed,
        )
    }

    suspend fun onContactUpdated(
        tenantId: TenantId,
        contactId: ContactId,
    ) {
        val cashflowStartDate = tenantRepository.getCashflowTrackingStartDate(tenantId)
        val entries = cashflowEntriesRepository.listOpenInvoiceEntriesByContact(tenantId, contactId, cashflowStartDate)
            .getOrDefault(emptyList())
        if (entries.isEmpty()) return

        val fromDate = Clock.System.now().toLocalDateTime(
            TimeZone.UTC,
        ).date.minus(DatePeriod(days = RECENT_TX_WINDOW_DAYS))
        val transactions = importedBankTransactionRepository.listRecentCandidatePool(tenantId, fromDate)
        if (transactions.isEmpty()) return

        matchingEngine.matchTransactions(
            tenantId = tenantId,
            transactions = transactions,
            triggerSource = AutoPaymentTriggerSource.ContactUpdated,
        )
    }
}
