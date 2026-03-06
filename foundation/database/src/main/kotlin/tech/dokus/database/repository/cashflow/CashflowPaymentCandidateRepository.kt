package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.CashflowPaymentCandidatesTable
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ImportedBankTransactionId
import tech.dokus.domain.ids.TenantId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

data class CashflowPaymentCandidateRecord(
    val cashflowEntryId: CashflowEntryId,
    val importedBankTransactionId: ImportedBankTransactionId,
    val score: Double,
    val tier: PaymentCandidateTier,
    val signalSnapshotJson: String? = null
)

@OptIn(ExperimentalUuidApi::class)
class CashflowPaymentCandidateRepository {
    suspend fun upsertBestCandidate(
        tenantId: TenantId,
        record: CashflowPaymentCandidateRecord
    ): Boolean = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        val entryUuid = record.cashflowEntryId.value.toJavaUuid()
        val txUuid = record.importedBankTransactionId.value.toJavaUuid()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val updated = CashflowPaymentCandidatesTable.update({
            (CashflowPaymentCandidatesTable.tenantId eq tenantUuid) and
                (CashflowPaymentCandidatesTable.cashflowEntryId eq entryUuid)
        }) {
            it[importedBankTransactionId] = txUuid
            it[score] = record.score.toBigDecimal()
            it[tier] = record.tier
            it[signalSnapshotJson] = record.signalSnapshotJson
            it[updatedAt] = now
        } > 0

        if (!updated) {
            CashflowPaymentCandidatesTable.insert {
                it[id] = UUID.randomUUID()
                it[CashflowPaymentCandidatesTable.tenantId] = tenantUuid
                it[cashflowEntryId] = entryUuid
                it[importedBankTransactionId] = txUuid
                it[score] = record.score.toBigDecimal()
                it[tier] = record.tier
                it[signalSnapshotJson] = record.signalSnapshotJson
                it[updatedAt] = now
            }
        }
        true
    }

    suspend fun getByEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): CashflowPaymentCandidateRecord? = newSuspendedTransaction {
        CashflowPaymentCandidatesTable.selectAll().where {
            (CashflowPaymentCandidatesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (CashflowPaymentCandidatesTable.cashflowEntryId eq cashflowEntryId.value.toJavaUuid())
        }.singleOrNull()?.let { row ->
            CashflowPaymentCandidateRecord(
                cashflowEntryId = CashflowEntryId.parse(row[CashflowPaymentCandidatesTable.cashflowEntryId].toString()),
                importedBankTransactionId =
                    ImportedBankTransactionId.parse(row[CashflowPaymentCandidatesTable.importedBankTransactionId].toString()),
                score = row[CashflowPaymentCandidatesTable.score].toDouble(),
                tier = row[CashflowPaymentCandidatesTable.tier],
                signalSnapshotJson = row[CashflowPaymentCandidatesTable.signalSnapshotJson]
            )
        }
    }

    suspend fun clearForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Int = newSuspendedTransaction {
        CashflowPaymentCandidatesTable.deleteWhere {
            (CashflowPaymentCandidatesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (CashflowPaymentCandidatesTable.cashflowEntryId eq cashflowEntryId.value.toJavaUuid())
        }
    }
}
