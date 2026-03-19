package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable

/**
 * Blacklisted transaction ↔ document pairs.
 * When a user rejects a suggested match, the pair is recorded here
 * and the RejectedGuard signal applies a strong negative penalty.
 */
object RejectedMatchPairsTable : UUIDTable("rejected_match_pairs") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val transactionId = uuid("transaction_id").references(
        BankTransactionsTable.id,
        onDelete = ReferenceOption.CASCADE,
    )
    val documentId = uuid("document_id").references(
        DocumentsTable.id,
        onDelete = ReferenceOption.CASCADE,
    )
    val rejectedAt = datetime("rejected_at").defaultExpression(CurrentDateTime)
    val rejectedBy = uuid("rejected_by").nullable()

    init {
        uniqueIndex("ux_rejected_match_pairs", tenantId, transactionId, documentId)
    }
}
