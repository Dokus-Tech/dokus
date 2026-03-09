package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.AutoPaymentAuditEventsTable
import tech.dokus.domain.enums.AutoPaymentDecision
import tech.dokus.domain.enums.AutoPaymentTriggerSource
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

data class AutoPaymentAuditEventCreate(
    val tenantId: TenantId,
    val triggerSource: AutoPaymentTriggerSource,
    val decision: AutoPaymentDecision,
    val invoiceId: InvoiceId? = null,
    val cashflowEntryId: CashflowEntryId? = null,
    val transactionId: BankTransactionId? = null,
    val score: Double? = null,
    val margin: Double? = null,
    val reasonsJson: String? = null,
    val rulesJson: String? = null,
    val actorUserId: UUID? = null,
)

@OptIn(ExperimentalUuidApi::class)
class AutoPaymentAuditRepository {
    suspend fun append(event: AutoPaymentAuditEventCreate): Unit = newSuspendedTransaction {
        AutoPaymentAuditEventsTable.insert {
            it[id] = UUID.randomUUID()
            it[tenantId] = event.tenantId.value.toJavaUuid()
            it[triggerSource] = event.triggerSource
            it[decision] = event.decision
            it[invoiceId] = event.invoiceId?.value?.toJavaUuid()
            it[cashflowEntryId] = event.cashflowEntryId?.value?.toJavaUuid()
            it[importedBankTransactionId] = event.transactionId?.value?.toJavaUuid()
            it[score] = event.score?.toBigDecimal()
            it[margin] = event.margin?.toBigDecimal()
            it[reasonsJson] = event.reasonsJson
            it[rulesJson] = event.rulesJson
            it[actorUserId] = event.actorUserId
        }
    }
}
