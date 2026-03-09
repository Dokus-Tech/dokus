package tech.dokus.backend.services.cashflow

import java.security.MessageDigest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.banking.BankTransactionCreate
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BankStatementTransactionDraftRow
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

private const val RowConfidenceThreshold = 0.90
private const val StrongThreshold = 0.88
private const val PossibleThreshold = 0.70
private const val BestMarginThreshold = 0.08
private const val AmountToleranceMinor = 100L // 1.00 EUR
private const val DueDateWindowDays = 30

data class DiscardedBankStatementRow(
    val index: Int,
    val reason: String
)

data class BankStatementProcessingResult(
    val sanitizedDraft: BankStatementDraftData,
    val validRows: Int,
    val discardedRows: List<DiscardedBankStatementRow>
)

class BankStatementMatchingService(
    private val importedBankTransactionRepository: BankTransactionRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository,
    private val contactRepository: ContactRepository
) {
    private val logger = loggerFor()

    suspend fun processAndMatch(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: BankStatementDraftData
    ): BankStatementProcessingResult {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val discarded = mutableListOf<DiscardedBankStatementRow>()
        val validRows = mutableListOf<BankStatementTransactionDraftRow>()
        val inserts = mutableListOf<BankTransactionCreate>()

        draftData.transactions.forEachIndexed { index, row ->
            val date = row.transactionDate
            if (date == null) {
                discarded += DiscardedBankStatementRow(index, "missing_date")
                return@forEachIndexed
            }
            if (date > today) {
                discarded += DiscardedBankStatementRow(index, "date_in_future")
                return@forEachIndexed
            }

            val amount = row.signedAmount
            if (amount == null || amount.isZero) {
                discarded += DiscardedBankStatementRow(index, "invalid_or_zero_amount")
                return@forEachIndexed
            }

            val confidence = row.rowConfidence
            if (confidence < RowConfidenceThreshold) {
                discarded += DiscardedBankStatementRow(index, "low_confidence")
                return@forEachIndexed
            }

            val normalizedStructuredCommunication = normalizeStructuredCommunication(row.structuredCommunicationRaw)
            val dedupHash = hashRow(
                date = date,
                amount = amount,
                description = row.descriptionRaw,
                structuredCommunication = row.structuredCommunicationRaw,
                counterpartyName = row.counterpartyName
            )

            validRows += row
            inserts += BankTransactionCreate(
                dedupHash = dedupHash,
                transactionDate = date,
                signedAmount = amount,
                counterpartyName = row.counterpartyName,
                counterpartyIban = row.counterpartyIban?.value,
                structuredCommunicationRaw = row.structuredCommunicationRaw,
                normalizedStructuredCommunication = normalizedStructuredCommunication,
                descriptionRaw = row.descriptionRaw,
            )
        }

        importedBankTransactionRepository.replaceForDocument(
            tenantId = tenantId,
            documentId = documentId,
            rows = inserts
        )

        if (validRows.isNotEmpty()) {
            runMatching(tenantId = tenantId, documentId = documentId)
        }

        discarded.forEach {
            logger.info(
                "Discarded bank statement row for document {} at index {} due to {}",
                documentId,
                it.index,
                it.reason
            )
        }

        return BankStatementProcessingResult(
            sanitizedDraft = draftData.copy(transactions = validRows),
            validRows = validRows.size,
            discardedRows = discarded
        )
    }

    /**
     * Returns candidate transactions for a cashflow entry.
     * Combines NeedsReview suggestions with all selectable (unmatched) transactions.
     */
    suspend fun getPaymentCandidates(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): List<BankTransactionDto> {
        val candidates = importedBankTransactionRepository.listCandidatesForEntry(tenantId, cashflowEntryId)
        val selectable = importedBankTransactionRepository.listSelectable(tenantId)
        val candidateIds = candidates.map { it.id }.toSet()
        return candidates + selectable.filter { it.id !in candidateIds }
    }

    private suspend fun runMatching(
        tenantId: TenantId,
        documentId: DocumentId
    ) {
        val transactions = importedBankTransactionRepository.listByDocument(tenantId, documentId)
            .filter { it.status == BankTransactionStatus.Unmatched }
        if (transactions.isEmpty()) return

        val openEntries = cashflowEntriesRepository.listEntries(
            tenantId = tenantId,
            statuses = listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
        ).getOrDefault(emptyList()).filter { !it.remainingAmount.isZero }
        if (openEntries.isEmpty()) return
        val invoiceStructuredReferenceBySourceId = loadInvoiceStructuredReferenceMap(tenantId, openEntries)

        val contactById = mutableMapOf<String, tech.dokus.domain.model.contact.ContactDto?>()
        suspend fun resolveContact(entry: CashflowEntry): tech.dokus.domain.model.contact.ContactDto? {
            val contactId = entry.contactId ?: return null
            val key = contactId.toString()
            if (contactById.containsKey(key)) return contactById[key]
            val resolved = contactRepository.getContact(contactId, tenantId).getOrNull()
            contactById[key] = resolved
            return resolved
        }

        val bestPerEntry = mutableMapOf<CashflowEntryId, MatchCandidate>()
        val transactionSuggestions = mutableListOf<TransactionSuggestion>()

        for (tx in transactions) {
            val scored = openEntries.mapNotNull { entry ->
                val contact = resolveContact(entry)
                scoreCandidate(
                    tx = tx,
                    entry = entry,
                    contactVat = contact?.vatNumber?.normalized,
                    contactIban = contact?.iban?.value,
                    invoiceStructuredReference = invoiceStructuredReferenceBySourceId[entry.sourceId]
                )
            }.sortedByDescending { it.score }

            val best = scored.firstOrNull() ?: continue
            if (best.score < PossibleThreshold) continue

            transactionSuggestions += TransactionSuggestion(
                transactionId = tx.id,
                entryId = best.entry.id,
                score = best.score,
            )

            val currentBest = bestPerEntry[best.entry.id]
            if (currentBest == null || best.score > currentBest.score) {
                bestPerEntry[best.entry.id] = best
            }
        }

        bestPerEntry.keys.forEach { entryId ->
            importedBankTransactionRepository.clearCandidatesForEntry(tenantId, entryId)
        }

        transactionSuggestions.forEach { suggestion ->
            importedBankTransactionRepository.setMatchCandidate(
                tenantId = tenantId,
                transactionId = suggestion.transactionId,
                cashflowEntryId = suggestion.entryId,
                score = suggestion.score,
            )
        }
    }

    private data class TransactionSuggestion(
        val transactionId: tech.dokus.domain.ids.BankTransactionId,
        val entryId: CashflowEntryId,
        val score: Double,
    )

    private data class MatchCandidate(
        val transaction: BankTransactionDto,
        val entry: CashflowEntry,
        val score: Double,
        val signalSnapshot: String,
    )

    private fun scoreCandidate(
        tx: BankTransactionDto,
        entry: CashflowEntry,
        contactVat: String?,
        contactIban: String?,
        invoiceStructuredReference: String?
    ): MatchCandidate? {
        if (!isSignCoherent(tx.signedAmount, entry.direction)) return null

        val absoluteAmountMinor = abs(tx.signedAmount.minor)
        val targetAmountMinor = entry.remainingAmount.minor
        val exactAmount = absoluteAmountMinor == targetAmountMinor
        val amountDelta = abs(absoluteAmountMinor - targetAmountMinor)
        val withinTolerance = amountDelta <= AmountToleranceMinor
        val dueDaysDistance = abs(tx.transactionDate.daysUntil(entry.eventDate))
        val withinDueWindow = dueDaysDistance <= DueDateWindowDays

        val normalizedTxStructuredRef = normalizeStructuredCommunication(tx.structuredCommunicationRaw)
        val normalizedEntryStructuredRef = invoiceStructuredReference ?: normalizeStructuredCommunication(entry.description)
        val structuredMatch = normalizedTxStructuredRef != null && normalizedTxStructuredRef == normalizedEntryStructuredRef
        val ibanMatch = exactAmount && tx.counterpartyIban?.value != null &&
            tx.counterpartyIban?.value == normalizedIban(contactIban)
        val vatMatch = exactAmount && !contactVat.isNullOrBlank() &&
            containsVatHint(tx.descriptionRaw, contactVat)

        val baseScore = when {
            exactAmount && structuredMatch -> 1.0
            exactAmount && ibanMatch -> 0.93
            exactAmount && vatMatch -> 0.88
            withinTolerance && withinDueWindow -> {
                val amountComponent = (AmountToleranceMinor - amountDelta).toDouble() / AmountToleranceMinor.toDouble()
                val dateComponent = (DueDateWindowDays - dueDaysDistance).toDouble() / DueDateWindowDays.toDouble()
                0.62 + (amountComponent * 0.18) + (dateComponent * 0.10)
            }
            else -> return null
        }

        val nameSimilarity = if (!tx.counterpartyName.isNullOrBlank() && !entry.contactName.isNullOrBlank()) {
            JaroWinkler.similarity(
                tx.counterpartyName!!.trim().lowercase(),
                entry.contactName!!.trim().lowercase()
            )
        } else {
            0.0
        }
        val modifier = if (nameSimilarity >= 0.95) 0.02 else 0.0
        val score = (baseScore + modifier).coerceAtMost(1.0)

        return MatchCandidate(
            transaction = tx,
            entry = entry,
            score = score,
            signalSnapshot = buildSignalSnapshot(
                exactAmount = exactAmount,
                structuredMatch = structuredMatch,
                ibanMatch = ibanMatch,
                vatMatch = vatMatch,
                amountDelta = amountDelta,
                dueDaysDistance = dueDaysDistance,
                nameSimilarity = nameSimilarity
            )
        )
    }

    private fun buildSignalSnapshot(
        exactAmount: Boolean,
        structuredMatch: Boolean,
        ibanMatch: Boolean,
        vatMatch: Boolean,
        amountDelta: Long,
        dueDaysDistance: Int,
        nameSimilarity: Double
    ): String {
        return """{"exactAmount":$exactAmount,"structuredMatch":$structuredMatch,"ibanMatch":$ibanMatch,"vatMatch":$vatMatch,"amountDeltaMinor":$amountDelta,"dueDaysDistance":$dueDaysDistance,"nameSimilarity":${"%.4f".format(
            nameSimilarity
        )}}"""
    }

    private fun isSignCoherent(amount: Money, direction: CashflowDirection): Boolean {
        return when (direction) {
            CashflowDirection.In -> amount.isPositive
            CashflowDirection.Out -> amount.isNegative
            CashflowDirection.Neutral -> false
        }
    }

    private fun normalizedIban(value: String?): String? = Iban.from(value)?.value

    private fun normalizeStructuredCommunication(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return raw.trim().uppercase().replace(Regex("\\s+"), "")
    }

    private fun containsVatHint(text: String?, vat: String): Boolean {
        if (text.isNullOrBlank()) return false
        val normalizedVat = vat.uppercase().replace(Regex("[^A-Z0-9]"), "")
        val normalizedText = text.uppercase().replace(Regex("[^A-Z0-9]"), "")
        return normalizedText.contains(normalizedVat)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun loadInvoiceStructuredReferenceMap(
        tenantId: TenantId,
        entries: List<CashflowEntry>
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
            InvoicesTable.structuredCommunication
        ).where {
            (InvoicesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoicesTable.id inList invoiceSourceIds.toList())
        }.mapNotNull { row ->
            val structured = normalizeStructuredCommunication(row[InvoicesTable.structuredCommunication])
                ?: return@mapNotNull null
            row[InvoicesTable.id].value.toString() to structured
        }.toMap()
    }

    private fun hashRow(
        date: LocalDate,
        amount: Money,
        description: String?,
        structuredCommunication: String?,
        counterpartyName: String?
    ): String {
        val raw = listOf(
            date.toString(),
            amount.minor.toString(),
            description?.trim().orEmpty(),
            structuredCommunication?.trim().orEmpty(),
            counterpartyName?.trim().orEmpty()
        ).joinToString("|")
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
