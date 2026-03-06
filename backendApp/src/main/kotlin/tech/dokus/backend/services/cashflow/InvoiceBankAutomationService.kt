package tech.dokus.backend.services.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CashflowPaymentCandidateRecord
import tech.dokus.database.repository.cashflow.CashflowPaymentCandidateRepository
import tech.dokus.database.repository.cashflow.ImportedBankTransactionRepository
import tech.dokus.database.repository.cashflow.InvoiceBankMatchLinkRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.ImportedBankTransactionDto
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

private const val AUTO_PAY_ENABLED = false
private const val AUTO_MATCH_THRESHOLD = 0.95
private const val MARGIN_THRESHOLD = 0.10
private const val POSSIBLE_THRESHOLD = 0.70
private const val AMOUNT_TOLERANCE_MINOR = 100L
private const val ALLOW_FEE_TOLERANCE_WITH_STRONG_REFERENCE = false
private const val NAME_SIMILARITY_THRESHOLD = 0.93
private const val DUE_DATE_WINDOW_DAYS = 15
private const val ISSUE_DATE_WINDOW_DAYS = 60
private const val RECENT_TX_WINDOW_DAYS = 120

private data class InvoiceMeta(
    val id: UUID,
    val status: InvoiceStatus,
    val structuredReference: String?,
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val paidAmount: Money,
    val totalAmount: Money,
)

private data class Candidate(
    val transaction: ImportedBankTransactionDto,
    val entry: CashflowEntry,
    val score: Double,
    val reasons: List<String>,
    val rules: List<String>,
    val hardReference: Boolean,
    val exactAmount: Boolean,
)

class InvoiceBankAutomationService(
    private val importedBankTransactionRepository: ImportedBankTransactionRepository,
    private val cashflowPaymentCandidateRepository: CashflowPaymentCandidateRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val contactRepository: ContactRepository,
    private val autoPaymentService: AutoPaymentService,
    private val invoiceBankMatchLinkRepository: InvoiceBankMatchLinkRepository,
) {
    private val logger = loggerFor()

    suspend fun onBankStatementImported(
        tenantId: TenantId,
        documentId: DocumentId
    ) {
        val transactions = importedBankTransactionRepository.listByDocument(tenantId, documentId)
        val entries = cashflowEntriesRepository.listOpenInvoiceEntries(tenantId).getOrDefault(emptyList())
        runMatchingAndAutomation(
            tenantId = tenantId,
            transactions = transactions,
            candidateEntries = entries,
            triggerSource = AutoPaymentTriggerSource.BankImport
        )
    }

    suspend fun onInvoiceConfirmed(
        tenantId: TenantId,
        entryId: CashflowEntryId
    ) {
        val entry = cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull() ?: return
        if (entry.sourceType != tech.dokus.domain.enums.CashflowSourceType.Invoice) return
        val fromDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.minus(DatePeriod(days = RECENT_TX_WINDOW_DAYS))
        val transactions = importedBankTransactionRepository.listRecentCandidatePool(tenantId, fromDate)
        runMatchingAndAutomation(
            tenantId = tenantId,
            transactions = transactions,
            candidateEntries = listOf(entry),
            triggerSource = AutoPaymentTriggerSource.InvoiceConfirmed
        )
    }

    suspend fun onContactUpdated(
        tenantId: TenantId,
        contactId: ContactId
    ) {
        val entries = cashflowEntriesRepository.listOpenInvoiceEntriesByContact(tenantId, contactId)
            .getOrDefault(emptyList())
        if (entries.isEmpty()) return
        val fromDate = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.minus(DatePeriod(days = RECENT_TX_WINDOW_DAYS))
        val transactions = importedBankTransactionRepository.listRecentCandidatePool(tenantId, fromDate)
        runMatchingAndAutomation(
            tenantId = tenantId,
            transactions = transactions,
            candidateEntries = entries,
            triggerSource = AutoPaymentTriggerSource.ContactUpdated
        )
    }

    private suspend fun runMatchingAndAutomation(
        tenantId: TenantId,
        transactions: List<ImportedBankTransactionDto>,
        candidateEntries: List<CashflowEntry>,
        triggerSource: AutoPaymentTriggerSource
    ) {
        if (transactions.isEmpty() || candidateEntries.isEmpty()) return

        val filteredEntries = candidateEntries.filter { !it.remainingAmount.isZero }
        if (filteredEntries.isEmpty()) return

        filteredEntries.forEach { entry ->
            importedBankTransactionRepository.clearSuggestionsForEntry(tenantId, entry.id)
            cashflowPaymentCandidateRepository.clearForEntry(tenantId, entry.id)
        }

        val invoiceMeta = loadInvoiceMeta(tenantId, filteredEntries)
        val contactById = mutableMapOf<String, tech.dokus.domain.model.contact.ContactDto?>()

        suspend fun resolveContact(entry: CashflowEntry): tech.dokus.domain.model.contact.ContactDto? {
            val contactId = entry.contactId ?: return null
            val key = contactId.toString()
            if (contactById.containsKey(key)) return contactById[key]
            val contact = contactRepository.getContact(contactId, tenantId).getOrNull()
            contactById[key] = contact
            return contact
        }

        val bestPerEntry = mutableMapOf<CashflowEntryId, Candidate>()

        for (tx in transactions) {
            if (tx.status == tech.dokus.domain.enums.ImportedBankTransactionStatus.Linked) continue

            val scored = filteredEntries.mapNotNull { entry ->
                val meta = invoiceMeta[entry.sourceId] ?: return@mapNotNull null
                if (meta.status in listOf(InvoiceStatus.Paid, InvoiceStatus.Cancelled, InvoiceStatus.Refunded)) {
                    return@mapNotNull null
                }
                val contact = resolveContact(entry)
                scoreCandidate(
                    tx = tx,
                    entry = entry,
                    invoice = meta,
                    contactIban = contact?.iban?.value
                )
            }.sortedByDescending { it.score }

            val best = scored.firstOrNull() ?: continue
            val second = scored.getOrNull(1)
            val margin = best.score - (second?.score ?: 0.0)
            val aboveThreshold = scored.count { it.score >= AUTO_MATCH_THRESHOLD }
            val isAutoMatch = best.score >= AUTO_MATCH_THRESHOLD && margin >= MARGIN_THRESHOLD && aboveThreshold == 1

            val tier = when {
                isAutoMatch -> PaymentCandidateTier.Strong
                best.score >= POSSIBLE_THRESHOLD -> PaymentCandidateTier.Possible
                else -> null
            } ?: continue

            importedBankTransactionRepository.setSuggestion(
                tenantId = tenantId,
                transactionId = tx.id,
                cashflowEntryId = best.entry.id,
                score = best.score,
                tier = tier
            )

            val currentBest = bestPerEntry[best.entry.id]
            if (currentBest == null || best.score > currentBest.score) {
                bestPerEntry[best.entry.id] = best
            }

            if (!isAutoMatch) {
                continue
            }

            val reasonsJson = Json.encodeToString(best.reasons)
            val rulesJson = Json.encodeToString(best.rules)

            val invoiceId = UUID.fromString(best.entry.sourceId)
            val linkId = invoiceBankMatchLinkRepository.upsertAutoMatched(
                tenantId = tenantId,
                invoiceId = tech.dokus.domain.ids.InvoiceId.parse(invoiceId.toString()),
                cashflowEntryId = best.entry.id,
                transactionId = tx.id,
                confidenceScore = best.score,
                scoreMargin = margin,
                reasonsJson = reasonsJson,
                rulesJson = rulesJson
            )
            logger.debug(
                "Auto-matched invoice-bank pair: tenant={}, entry={}, tx={}, link={}",
                tenantId,
                best.entry.id,
                tx.id,
                linkId
            )

            if (AUTO_PAY_ENABLED && best.exactAmount) {
                autoPaymentService.applyAutoPayment(
                    tenantId = tenantId,
                    entry = best.entry,
                    transaction = tx,
                    confidenceScore = best.score,
                    scoreMargin = margin,
                    reasonsJson = reasonsJson,
                    rulesJson = rulesJson,
                    triggerSource = triggerSource
                ).onFailure {
                    logger.warn(
                        "Auto-payment failed for tenant={}, entry={}, tx={}: {}",
                        tenantId,
                        best.entry.id,
                        tx.id,
                        it.message
                    )
                }
            }
        }

        bestPerEntry.values.forEach { candidate ->
            val tier = if (candidate.score >= AUTO_MATCH_THRESHOLD) PaymentCandidateTier.Strong else PaymentCandidateTier.Possible
            cashflowPaymentCandidateRepository.upsertBestCandidate(
                tenantId = tenantId,
                record = CashflowPaymentCandidateRecord(
                    cashflowEntryId = candidate.entry.id,
                    importedBankTransactionId = candidate.transaction.id,
                    score = candidate.score,
                    tier = tier,
                    signalSnapshotJson = Json.encodeToString(candidate.rules)
                )
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun loadInvoiceMeta(
        tenantId: TenantId,
        entries: List<CashflowEntry>
    ): Map<String, InvoiceMeta> = newSuspendedTransaction {
        val invoiceIds = entries.mapNotNull { entry ->
            if (entry.sourceType != tech.dokus.domain.enums.CashflowSourceType.Invoice) return@mapNotNull null
            runCatching { UUID.fromString(entry.sourceId) }.getOrNull()
        }.toSet()
        if (invoiceIds.isEmpty()) return@newSuspendedTransaction emptyMap()

        InvoicesTable.select(
            InvoicesTable.id,
            InvoicesTable.status,
            InvoicesTable.structuredCommunication,
            InvoicesTable.invoiceNumber,
            InvoicesTable.issueDate,
            InvoicesTable.paidAmount,
            InvoicesTable.totalAmount,
        ).where {
            (InvoicesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoicesTable.id inList invoiceIds.toList())
        }.associate { row ->
            row[InvoicesTable.id].value.toString() to InvoiceMeta(
                id = row[InvoicesTable.id].value,
                status = row[InvoicesTable.status],
                structuredReference = normalizeStructuredCommunication(row[InvoicesTable.structuredCommunication]),
                invoiceNumber = row[InvoicesTable.invoiceNumber],
                issueDate = row[InvoicesTable.issueDate],
                paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
                totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount])
            )
        }
    }

    private fun scoreCandidate(
        tx: ImportedBankTransactionDto,
        entry: CashflowEntry,
        invoice: InvoiceMeta,
        contactIban: String?
    ): Candidate? {
        if (!isSignCoherent(tx.signedAmount, entry.direction)) return null

        val absoluteAmountMinor = abs(tx.signedAmount.minor)
        val targetAmountMinor = entry.remainingAmount.minor
        val amountDelta = abs(absoluteAmountMinor - targetAmountMinor)
        val exactAmount = amountDelta == 0L

        val normalizedDescription = tx.descriptionRaw?.uppercase().orEmpty()
        val normalizedInvoiceNumber = invoice.invoiceNumber.uppercase().replace(" ", "")
        val normalizedTxStructuredRef = normalizeStructuredCommunication(tx.structuredCommunicationRaw)
        val structuredMatch = normalizedTxStructuredRef != null && normalizedTxStructuredRef == invoice.structuredReference
        val invoiceNumberMatch = normalizedInvoiceNumber.isNotBlank() &&
            normalizedDescription.replace(" ", "").contains(normalizedInvoiceNumber)

        val normalizedTxIban = tx.counterpartyIban?.value
        val normalizedContactIban = normalizeIban(contactIban)
        val ibanMatch = normalizedTxIban != null && normalizedTxIban == normalizedContactIban

        val hardReference = structuredMatch || invoiceNumberMatch
        val hardSignal = hardReference || ibanMatch
        if (!hardSignal) return null

        val nameSimilarity = if (!tx.counterpartyName.isNullOrBlank() && !entry.contactName.isNullOrBlank()) {
            JaroWinkler.similarity(
                tx.counterpartyName!!.trim().lowercase(),
                entry.contactName!!.trim().lowercase()
            )
        } else {
            0.0
        }
        val softName = nameSimilarity >= NAME_SIMILARITY_THRESHOLD
        val dueDaysDistance = abs(tx.transactionDate.daysUntil(entry.eventDate))
        val issueDaysDistance = abs(tx.transactionDate.daysUntil(invoice.issueDate))
        val softDate = dueDaysDistance <= DUE_DATE_WINDOW_DAYS || issueDaysDistance <= ISSUE_DATE_WINDOW_DAYS
        if (!softName && !softDate) return null

        val toleranceAllowed = ALLOW_FEE_TOLERANCE_WITH_STRONG_REFERENCE && hardReference && amountDelta <= AMOUNT_TOLERANCE_MINOR
        if (!exactAmount && !toleranceAllowed) return null

        val reasons = mutableListOf<String>()
        val rules = mutableListOf<String>()

        var score = 0.0
        if (structuredMatch) {
            score += 0.84
            reasons += "structured_reference_match"
            rules += "hard:structured_reference"
        } else if (invoiceNumberMatch) {
            score += 0.84
            reasons += "invoice_number_match"
            rules += "hard:invoice_number"
        } else if (ibanMatch) {
            score += 0.80
            reasons += "counterparty_iban_match"
            rules += "hard:iban"
        }

        if (exactAmount) {
            score += 0.10
            reasons += "exact_amount"
            rules += "amount:exact"
        } else {
            score += 0.06
            reasons += "fee_tolerance_amount"
            rules += "amount:tolerance"
        }

        if (softName) {
            score += 0.04
            reasons += "name_similarity_high"
            rules += "soft:name"
        }
        if (softDate) {
            score += 0.03
            reasons += "date_proximity"
            rules += "soft:date"
        }

        val cappedScore = score.coerceAtMost(1.0)
        if (cappedScore < POSSIBLE_THRESHOLD) return null

        return Candidate(
            transaction = tx,
            entry = entry,
            score = cappedScore,
            reasons = reasons,
            rules = rules,
            hardReference = hardReference,
            exactAmount = exactAmount
        )
    }

    private fun isSignCoherent(amount: Money, direction: CashflowDirection): Boolean {
        return when (direction) {
            CashflowDirection.In -> amount.isPositive
            CashflowDirection.Out -> amount.isNegative
            CashflowDirection.Neutral -> false
        }
    }

    private fun normalizeStructuredCommunication(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().uppercase().replace(Regex("\\s+"), "")
    }

    private fun normalizeIban(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.uppercase().replace(Regex("[^A-Z0-9]"), "")
    }
}
