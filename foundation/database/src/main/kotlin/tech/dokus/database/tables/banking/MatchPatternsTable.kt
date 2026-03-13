package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable

private const val IbanLength = 34

/**
 * Learned counterparty IBAN → contact mappings from confirmed matches.
 * Used by the HistoricalPattern signal in the Bayesian scoring engine.
 */
object MatchPatternsTable : UUIDTable("match_patterns") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE)
    val counterpartyIban = varchar("counterparty_iban", IbanLength)
    val contactId = uuid("contact_id").references(ContactsTable.id, onDelete = ReferenceOption.CASCADE)
    val matchCount = integer("match_count").default(1)
    val lastMatchedAt = datetime("last_matched_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_match_patterns_iban_contact", tenantId, counterpartyIban, contactId)
    }
}
