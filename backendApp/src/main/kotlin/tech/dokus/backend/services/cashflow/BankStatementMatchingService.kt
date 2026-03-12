package tech.dokus.backend.services.cashflow

import kotlinx.datetime.daysUntil
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.backend.services.banking.BankStatementProcessingService
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

private const val DueDateWindowDays = 30
private const val AmountToleranceMinor = 100L // 1.00 EUR

/**
 * Match classification for a transaction × entry pair.
 */
private enum class MatchClassification {
    /** Strong evidence — auto-match and trigger payment */
    AutoMatch,
    /** Decent evidence but needs human review */
    NeedsReview,
}

class BankStatementMatchingService(
    private val bankTransactionRepository: BankTransactionRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val contactRepository: ContactRepository,
    private val autoPaymentService: AutoPaymentService,
) {
    private val logger = loggerFor()

    /**
     * Returns candidate transactions for a cashflow entry.
     * Combines NeedsReview suggestions with all selectable (unmatched) transactions.
     */
    suspend fun getPaymentCandidates(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId,
    ): List<BankTransactionDto> {
        val candidates = bankTransactionRepository.listCandidatesForEntry(tenantId, cashflowEntryId)
        val selectable = bankTransactionRepository.listSelectable(tenantId)
        val candidateIds = candidates.map { it.id }.toSet()
        return candidates + selectable.filter { it.id !in candidateIds }
    }

    /**
     * Run matching for transactions from a specific document against open cashflow entries.
     * Uses hard-evidence gates per spec §8.
     */
    suspend fun runMatching(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        val transactions = bankTransactionRepository.listByDocument(tenantId, documentId)
            .filter { it.status == BankTransactionStatus.Unmatched }
        if (transactions.isEmpty()) return

        matchTransactions(tenantId, transactions)
    }

    /**
     * Run matching for a list of unmatched transactions against open cashflow entries.
     * Reusable entry point for bidirectional triggers.
     */
    suspend fun matchTransactions(
        tenantId: TenantId,
        transactions: List<BankTransactionDto>,
    ) {
        val openEntries = cashflowEntriesRepository.listEntries(
            tenantId = tenantId,
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue),
        ).getOrDefault(emptyList()).filter { !it.remainingAmount.isZero }
        if (openEntries.isEmpty()) return

        val invoiceRefsBySourceId = loadInvoiceStructuredReferenceMap(tenantId, openEntries)

        val contactCache = mutableMapOf<String, tech.dokus.domain.model.contact.ContactDto?>()
        suspend fun resolveContact(entry: CashflowEntry): tech.dokus.domain.model.contact.ContactDto? {
            val contactId = entry.contactId ?: return null
            val key = contactId.toString()
            if (contactCache.containsKey(key)) return contactCache[key]
            val resolved = contactRepository.getContact(contactId, tenantId).getOrNull()
            contactCache[key] = resolved
            return resolved
        }

        for (tx in transactions) {
            val candidates = openEntries.mapNotNull { entry ->
                val contact = resolveContact(entry)
                evaluateEvidence(
                    tx = tx,
                    entry = entry,
                    contactIban = contact?.iban?.value,
                    invoiceStructuredReference = invoiceRefsBySourceId[entry.sourceId],
                )
            }.sortedByDescending { it.score }

            val best = candidates.firstOrNull() ?: continue

            val classification = classifyMatch(
                best = best,
                candidateCount = candidates.size,
                trust = tx.statementTrust,
            )

            when (classification) {
                MatchClassification.AutoMatch -> applyAutoMatch(tenantId, best)
                MatchClassification.NeedsReview -> applyNeedsReview(tenantId, best)
            }
        }
    }

    // ─── Evidence evaluation ──────────────────────────────────────────────

    private data class EvidenceResult(
        val tx: BankTransactionDto,
        val entry: CashflowEntry,
        val score: Double,
        val evidence: List<String>,
        val exactAmount: Boolean,
        val structuredCommMatch: Boolean,
        val counterpartyIbanMatch: Boolean,
        val withinDueWindow: Boolean,
    )

    private fun evaluateEvidence(
        tx: BankTransactionDto,
        entry: CashflowEntry,
        contactIban: String?,
        invoiceStructuredReference: String?,
    ): EvidenceResult? {
        if (!isSignCoherent(tx.signedAmount, entry.direction)) return null

        val absoluteAmountMinor = abs(tx.signedAmount.minor)
        val targetAmountMinor = entry.remainingAmount.minor
        val exactAmount = absoluteAmountMinor == targetAmountMinor
        val amountDelta = abs(absoluteAmountMinor - targetAmountMinor)
        val withinTolerance = amountDelta <= AmountToleranceMinor
        val dueDaysDistance = abs(tx.transactionDate.daysUntil(entry.eventDate))
        val withinDueWindow = dueDaysDistance <= DueDateWindowDays

        // Hard evidence signals
        val txStructuredRaw = (tx.communication as? TransactionCommunication.Structured)?.raw
        val normalizedTxRef = normalizeStructuredCommunication(txStructuredRaw)
        val normalizedEntryRef = invoiceStructuredReference
            ?: normalizeStructuredCommunication(entry.description)
        val structuredCommMatch = normalizedTxRef != null && normalizedTxRef == normalizedEntryRef

        val counterpartyIbanMatch = tx.counterparty.iban?.value != null &&
            tx.counterparty.iban?.value == normalizedIban(contactIban)

        // Collect evidence signals
        val evidence = mutableListOf<String>()
        if (exactAmount) evidence += "exact_amount"
        if (structuredCommMatch) evidence += "structured_comm_match"
        if (counterpartyIbanMatch) evidence += "counterparty_iban_match"
        if (withinDueWindow) evidence += "within_due_window"
        if (withinTolerance && !exactAmount) evidence += "amount_within_tolerance"

        val nameSimilarity = if (!tx.counterparty.name.isNullOrBlank() && !entry.contactName.isNullOrBlank()) {
            JaroWinkler.similarity(
                tx.counterparty.name!!.trim().lowercase(),
                entry.contactName!!.trim().lowercase(),
            )
        } else {
            0.0
        }
        if (nameSimilarity >= 0.90) evidence += "counterparty_name_match"

        // Score (used for ranking and review display)
        val baseScore = when {
            exactAmount && structuredCommMatch -> 1.0
            exactAmount && counterpartyIbanMatch -> 0.95
            exactAmount && nameSimilarity >= 0.90 && withinDueWindow -> 0.88
            withinTolerance && withinDueWindow -> {
                val amtPart = (AmountToleranceMinor - amountDelta).toDouble() / AmountToleranceMinor
                val datePart = (DueDateWindowDays - dueDaysDistance).toDouble() / DueDateWindowDays
                0.62 + (amtPart * 0.18) + (datePart * 0.10)
            }
            else -> return null
        }

        return EvidenceResult(
            tx = tx,
            entry = entry,
            score = baseScore.coerceAtMost(1.0),
            evidence = evidence,
            exactAmount = exactAmount,
            structuredCommMatch = structuredCommMatch,
            counterpartyIbanMatch = counterpartyIbanMatch,
            withinDueWindow = withinDueWindow,
        )
    }

    // ─── Evidence gates ───────────────────────────────────────────────────

    /**
     * Classify a match based on hard-evidence gates (spec §8).
     *
     * AUTO_MATCH requires:
     * - Trust >= HIGH
     * - Single candidate (no ambiguity)
     * - Strong evidence: (structuredComm + exactAmount) OR (iban + exactAmount + withinDueWindow)
     *
     * Everything else → NEEDS_REVIEW
     */
    private fun classifyMatch(
        best: EvidenceResult,
        candidateCount: Int,
        trust: StatementTrust,
    ): MatchClassification {
        if (trust != StatementTrust.High) return MatchClassification.NeedsReview
        if (candidateCount > 1) return MatchClassification.NeedsReview

        val hasStrongEvidence = (best.structuredCommMatch && best.exactAmount) ||
            (best.counterpartyIbanMatch && best.exactAmount && best.withinDueWindow)

        return if (hasStrongEvidence) {
            MatchClassification.AutoMatch
        } else {
            MatchClassification.NeedsReview
        }
    }

    // ─── Apply match decisions ────────────────────────────────────────────

    private suspend fun applyAutoMatch(tenantId: TenantId, result: EvidenceResult) {
        bankTransactionRepository.markMatched(
            tenantId = tenantId,
            transactionId = result.tx.id,
            cashflowEntryId = result.entry.id,
            matchedBy = MatchedBy.Auto,
            resolutionType = ResolutionType.Document,
            score = result.score,
            evidence = result.evidence,
        )

        runSuspendCatching {
            autoPaymentService.applyAutoPayment(
                tenantId = tenantId,
                entry = result.entry,
                transaction = result.tx,
                confidenceScore = result.score,
                scoreMargin = 0.0,
                reasonsJson = Json.encodeToString(result.evidence),
                rulesJson = Json.encodeToString(mapOf("gate" to "hard_evidence", "trust" to "HIGH")),
                triggerSource = tech.dokus.domain.enums.AutoPaymentTriggerSource.BankImport,
            )
        }.onFailure { e ->
            logger.warn(
                "Auto-payment failed for tx {} → entry {}: {}",
                result.tx.id, result.entry.id, e.message,
            )
        }

        logger.info(
            "Auto-matched tx {} → entry {} (score={}, evidence={})",
            result.tx.id, result.entry.id, result.score, result.evidence,
        )
    }

    private suspend fun applyNeedsReview(tenantId: TenantId, result: EvidenceResult) {
        bankTransactionRepository.setMatchCandidate(
            tenantId = tenantId,
            transactionId = result.tx.id,
            cashflowEntryId = result.entry.id,
            score = result.score,
            evidence = result.evidence,
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private fun isSignCoherent(amount: Money, direction: CashflowDirection): Boolean {
        return when (direction) {
            CashflowDirection.In -> amount.isPositive
            CashflowDirection.Out -> amount.isNegative
            CashflowDirection.Neutral -> false
        }
    }

    private fun normalizedIban(value: String?): String? = Iban.from(value)?.value

    private fun normalizeStructuredCommunication(raw: String?): String? =
        BankStatementProcessingService.normalizeStructuredCommunication(raw)

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun loadInvoiceStructuredReferenceMap(
        tenantId: TenantId,
        entries: List<CashflowEntry>,
    ): Map<String, String> = newSuspendedTransaction {
        val invoiceSourceIds = entries
            .asSequence()
            .filter { it.sourceType == CashflowSourceType.Invoice }
            .mapNotNull { entry ->
                runCatching { UUID.fromString(entry.sourceId) }.getOrNull()
            }
            .toSet()
        if (invoiceSourceIds.isEmpty()) return@newSuspendedTransaction emptyMap()

        InvoicesTable.select(
            InvoicesTable.id,
            InvoicesTable.structuredCommunication,
        ).where {
            (InvoicesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoicesTable.id inList invoiceSourceIds.toList())
        }.mapNotNull { row ->
            val structured = normalizeStructuredCommunication(row[InvoicesTable.structuredCommunication])
                ?: return@mapNotNull null
            row[InvoicesTable.id].value.toString() to structured
        }.toMap()
    }
}
