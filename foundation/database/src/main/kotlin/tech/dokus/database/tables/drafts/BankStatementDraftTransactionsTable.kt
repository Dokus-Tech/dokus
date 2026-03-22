package tech.dokus.database.tables.drafts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime

object BankStatementDraftTransactionsTable : UUIDTable("bank_statement_draft_transactions") {
    val draftId = uuid("draft_id").references(BankStatementDraftsTable.id, onDelete = ReferenceOption.CASCADE).index()
    val transactionDate = date("transaction_date").nullable()
    val signedAmount = decimal("signed_amount", 19, 4).nullable()
    val counterpartyName = text("counterparty_name").nullable()
    val counterpartyVat = varchar("counterparty_vat", 50).nullable()
    val counterpartyIban = varchar("counterparty_iban", 34).nullable()
    val counterpartyBic = varchar("counterparty_bic", 11).nullable()
    val counterpartyEmail = varchar("counterparty_email", 255).nullable()
    val counterpartyCompanyNumber = varchar("counterparty_company_number", 50).nullable()
    val structuredCommunicationRaw = varchar("structured_communication_raw", 255).nullable()
    val freeCommunication = text("free_communication").nullable()
    val descriptionRaw = text("description_raw").nullable()
    val rowConfidence = double("row_confidence").default(0.0)
    val largeAmountFlag = bool("large_amount_flag").default(false)
    val excluded = bool("excluded").default(false)
    val potentialDuplicate = bool("potential_duplicate").default(false)
    val sortOrder = integer("sort_order").default(0)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
