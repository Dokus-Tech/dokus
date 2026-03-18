package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Tracks imported bank statements for dedup and audit.
 * Each row represents one imported file (PDF, CODA, MT940, etc.).
 */
object BankStatementsTable : UUIDTable("bank_statements") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE,
    ).index()

    val bankAccountId = uuid("bank_account_id")
        .references(BankAccountsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    val statementSource = dbEnumeration<BankTransactionSource>("source")
    val statementTrust = dbEnumeration<StatementTrust>("statement_trust").default(StatementTrust.Low)

    /** SHA-256 of the raw file bytes — strong dedup key. Null when hash unavailable. */
    val fileHash = varchar("file_hash", 64).nullable()

    val accountIban = varchar("account_iban", 34).nullable()
    val periodStart = date("period_start").nullable()
    val periodEnd = date("period_end").nullable()
    val openingBalance = decimal("opening_balance", 19, 4).nullable()
    val closingBalance = decimal("closing_balance", 19, 4).nullable()
    val transactionCount = integer("transaction_count").default(0)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, fileHash)
        index(false, tenantId, accountIban, periodEnd)
    }
}
