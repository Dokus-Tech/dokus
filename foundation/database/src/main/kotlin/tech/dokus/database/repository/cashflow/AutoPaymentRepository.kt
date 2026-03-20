package tech.dokus.database.repository.cashflow

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.PaymentCreatedBy
import java.util.UUID

class AutoPaymentRepository {

    /**
     * Upsert a match link between a document and a bank transaction.
     * If a link already exists for the (tenant, document, transaction) triple, it is updated.
     * Otherwise a new row is inserted.
     *
     * Must be called from within an existing Exposed transaction.
     *
     * @return the UUID of the inserted or updated row.
     */
    fun upsertMatchLink(
        tenantUuid: UUID,
        invoiceUuid: UUID,
        entryUuid: UUID,
        txUuid: UUID,
        confidenceScore: Double,
        scoreMargin: Double,
        reasonsJson: String,
        rulesJson: String,
        now: LocalDateTime,
        status: AutoMatchStatus
    ): UUID {
        val existing = TransactionMatchLinksTable.selectAll().where {
            (TransactionMatchLinksTable.tenantId eq tenantUuid) and
                (TransactionMatchLinksTable.documentId eq invoiceUuid) and
                (TransactionMatchLinksTable.importedBankTransactionId eq txUuid)
        }.singleOrNull()

        return if (existing == null) {
            val id = UUID.randomUUID()
            TransactionMatchLinksTable.insertAndGetId {
                it[TransactionMatchLinksTable.id] = id
                it[tenantId] = tenantUuid
                it[documentId] = invoiceUuid
                it[cashflowEntryId] = entryUuid
                it[importedBankTransactionId] = txUuid
                it[TransactionMatchLinksTable.status] = status
                it[createdBy] = PaymentCreatedBy.Auto
                it[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[TransactionMatchLinksTable.reasonsJson] = reasonsJson
                it[TransactionMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[createdAt] = now
                it[updatedAt] = now
            }.value
        } else {
            val existingId = existing[TransactionMatchLinksTable.id].value
            TransactionMatchLinksTable.update({ TransactionMatchLinksTable.id eq existingId }) {
                it[cashflowEntryId] = entryUuid
                it[TransactionMatchLinksTable.status] = status
                it[TransactionMatchLinksTable.confidenceScore] = confidenceScore.toBigDecimal()
                it[TransactionMatchLinksTable.scoreMargin] = scoreMargin.toBigDecimal()
                it[TransactionMatchLinksTable.reasonsJson] = reasonsJson
                it[TransactionMatchLinksTable.rulesJson] = rulesJson
                it[matchedAt] = now
                it[reversedAt] = null
                it[reversedByUserId] = null
                it[reversalReason] = null
                it[updatedAt] = now
            }
            existingId
        }
    }
}
