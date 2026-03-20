package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.ids.UserId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class AutoPaymentAuditRepository {

    /**
     * Insert an audit event into the auto_payment_audit_events table.
     * Must be called from within an existing Exposed transaction.
     */
    fun appendAudit(
        tenantUuid: UUID,
        triggerSource: AutoPaymentTriggerSource,
        decision: AutoPaymentDecision,
        invoiceId: UUID?,
        entryId: UUID?,
        txId: UUID?,
        score: Double?,
        margin: Double?,
        reasonsJson: String?,
        rulesJson: String?,
        actorUserId: UserId?
    ) {
        AutoPaymentAuditEventsTable.insertAndGetId {
            it[id] = UUID.randomUUID()
            it[tenantId] = tenantUuid
            it[AutoPaymentAuditEventsTable.triggerSource] = triggerSource
            it[AutoPaymentAuditEventsTable.decision] = decision
            it[AutoPaymentAuditEventsTable.invoiceId] = invoiceId
            it[cashflowEntryId] = entryId
            it[importedBankTransactionId] = txId
            it[AutoPaymentAuditEventsTable.score] = score?.toBigDecimal()
            it[AutoPaymentAuditEventsTable.margin] = margin?.toBigDecimal()
            it[AutoPaymentAuditEventsTable.reasonsJson] = reasonsJson
            it[AutoPaymentAuditEventsTable.rulesJson] = rulesJson
            it[AutoPaymentAuditEventsTable.actorUserId] = actorUserId?.value?.toJavaUuid()
        }
    }
}
