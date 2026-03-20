package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.MatchSignalType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.domain.model.CashflowEntryEntity
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.util.JaroWinkler
import kotlin.math.abs
import kotlin.math.exp

/**
 * Bayesian scorer: evaluates 8 weighted signals per (transaction, entry) pair,
 * aggregates log-odds, and converts to a calibrated 0–1 probability via sigmoid.
 */
class MatchScorer(
    private val matchingRepository: MatchingRepository,
) {

    /**
     * Score a single (transaction, candidate) pair.
     * Returns null if the pair is fundamentally incompatible.
     */
    suspend fun score(
        tx: BankTransactionEntity,
        candidate: MatchCandidate,
        invoiceMeta: InvoiceMatchMeta?,
        rejectedDocumentIds: Set<DocumentId>,
    ): ScoredCandidate? {
        val entry = candidate.entry

        val signals = mutableListOf<SignalResult>()
        var logOdds = MatchingConstants.PRIOR_LOG_ODDS
        var hasHardSignal = false

        // Reconstruct communication from flat entity fields
        val txCommunication = TransactionCommunication.from(tx.structuredCommunicationRaw, tx.freeCommunication)

        // ── Signal 1: OGM match ────────────────────────────────────────
        val ogmMatch = OgmValidator.matches(txCommunication, invoiceMeta?.structuredReference)
        signals += SignalResult(MatchSignalType.Ogm, ogmMatch, if (ogmMatch) MatchingConstants.WEIGHT_OGM else 0.0)
        if (ogmMatch) {
            logOdds += MatchingConstants.WEIGHT_OGM
            hasHardSignal = true
        }

        // ── Signal 2: Invoice reference in free-form text ──────────────
        val refMatch = ReferenceExtractor.containsInvoiceNumber(txCommunication, invoiceMeta?.invoiceNumber)
        signals += SignalResult(MatchSignalType.InvoiceRef, refMatch, if (refMatch) MatchingConstants.WEIGHT_INVOICE_REF else 0.0)
        if (refMatch) {
            logOdds += MatchingConstants.WEIGHT_INVOICE_REF
            hasHardSignal = true
        }

        // ── Signal 3: Counterparty IBAN match ──────────────────────────
        val ibanMatch = !tx.counterpartyIban?.value.isNullOrBlank() &&
            tx.counterpartyIban?.value == candidate.contactIban
        signals += SignalResult(MatchSignalType.CounterpartyIban, ibanMatch, if (ibanMatch) MatchingConstants.WEIGHT_COUNTERPARTY_IBAN else 0.0)
        if (ibanMatch) {
            logOdds += MatchingConstants.WEIGHT_COUNTERPARTY_IBAN
            hasHardSignal = true
        }

        // ── Signal 4: Amount match ─────────────────────────────────────
        val txAbsMinor = abs(tx.signedAmount.minor)
        val entryMinor = abs(entry.remainingAmount.minor)
        val amountDelta = abs(txAbsMinor - entryMinor)
        val exactAmount = amountDelta == 0L
        val closeAmount = !exactAmount && amountDelta <= MatchingConstants.AMOUNT_TOLERANCE_MINOR

        val amountWeight = when {
            exactAmount -> MatchingConstants.WEIGHT_AMOUNT_EXACT
            closeAmount -> MatchingConstants.WEIGHT_AMOUNT_CLOSE
            else -> 0.0
        }
        val amountFired = exactAmount || closeAmount
        signals += SignalResult(
            MatchSignalType.Amount, amountFired, amountWeight,
            detail = if (exactAmount) "exact" else if (closeAmount) "close:$amountDelta" else "none",
        )
        logOdds += amountWeight

        // ── Signal 5: Contact name similarity ──────────────────────────
        val nameSimilarity = if (!tx.counterpartyName.isNullOrBlank() && !candidate.contactName.isNullOrBlank()) {
            JaroWinkler.similarity(
                tx.counterpartyName!!.trim().lowercase(),
                candidate.contactName!!.trim().lowercase(),
            )
        } else {
            0.0
        }
        val strongName = nameSimilarity >= MatchingConstants.STRONG_NAME_THRESHOLD
        val weakName = !strongName && nameSimilarity >= MatchingConstants.WEAK_NAME_THRESHOLD
        val nameWeight = when {
            strongName -> MatchingConstants.WEIGHT_NAME_STRONG
            weakName -> MatchingConstants.WEIGHT_NAME_WEAK
            else -> 0.0
        }
        signals += SignalResult(
            MatchSignalType.ContactName, strongName || weakName, nameWeight,
            detail = "jw=%.3f".format(nameSimilarity),
        )
        logOdds += nameWeight

        // ── Signal 6: Date proximity (Gaussian decay) ──────────────────
        val daysDelta = abs(tx.transactionDate.toEpochDays() - entry.eventDate.toEpochDays())
        val dateWeight = if (daysDelta <= MatchingConstants.DATE_MAX_DAYS) {
            val gaussian = exp(-0.5 * (daysDelta.toDouble() / MatchingConstants.DATE_SIGMA_DAYS).let { it * it })
            MatchingConstants.WEIGHT_DATE_PROXIMITY * gaussian
        } else {
            0.0
        }
        val dateFired = dateWeight > 0.01
        signals += SignalResult(
            MatchSignalType.DateProximity, dateFired, dateWeight,
            detail = "days=$daysDelta",
        )
        logOdds += dateWeight

        // ── Signal 7: Historical pattern ───────────────────────────────
        val historicalFired = candidate.contactId != null &&
            !tx.counterpartyIban?.value.isNullOrBlank() &&
            checkHistoricalPattern(tx, candidate.contactId)
        val historicalWeight = if (historicalFired) MatchingConstants.WEIGHT_HISTORICAL_PATTERN else 0.0
        signals += SignalResult(MatchSignalType.HistoricalPattern, historicalFired, historicalWeight)
        logOdds += historicalWeight

        // ── Signal 8: Rejected guard ───────────────────────────────────
        val documentId = entry.documentId
        val isRejected = documentId != null && documentId in rejectedDocumentIds
        val rejectedWeight = if (isRejected) MatchingConstants.WEIGHT_REJECTED_GUARD else 0.0
        signals += SignalResult(MatchSignalType.RejectedGuard, isRejected, rejectedWeight)
        logOdds += rejectedWeight

        // ── Sigmoid conversion ─────────────────────────────────────────
        val probability = sigmoid(logOdds)

        if (probability < MatchingConstants.DISCARD_THRESHOLD) return null

        return ScoredCandidate(
            candidate = candidate,
            transaction = tx,
            score = probability,
            signals = signals,
            hasHardSignal = hasHardSignal,
        )
    }

    private suspend fun checkHistoricalPattern(
        tx: BankTransactionEntity,
        contactId: ContactId,
    ): Boolean {
        val iban = tx.counterpartyIban?.value ?: return false
        val patterns = matchingRepository.loadMatchPatterns(tx.tenantId, iban)
        return patterns.any { it.contactId == contactId }
    }

    companion object {
        private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))
    }
}
