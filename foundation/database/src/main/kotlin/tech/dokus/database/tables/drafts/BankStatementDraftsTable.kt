package tech.dokus.database.tables.drafts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.foundation.backend.database.dbEnumeration

object BankStatementDraftsTable : UUIDTable("bank_statement_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Neutral)
    val accountIban = varchar("account_iban", 34).nullable()
    val openingBalance = decimal("opening_balance", 19, 4).nullable()
    val closingBalance = decimal("closing_balance", 19, 4).nullable()
    val periodStart = date("period_start").nullable()
    val periodEnd = date("period_end").nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_bank_statement_drafts_document", tenantId, documentId)
    }
}
