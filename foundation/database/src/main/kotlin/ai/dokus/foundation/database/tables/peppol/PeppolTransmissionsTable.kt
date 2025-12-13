package ai.dokus.foundation.database.tables.peppol

import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.domain.enums.PeppolDocumentType
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Peppol document transmissions - tracks all sent and received Peppol documents.
 * CRITICAL: All queries MUST filter by tenant_id
 */
object PeppolTransmissionsTable : UUIDTable("peppol_transmissions") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Transmission details
    val direction = dbEnumeration<PeppolTransmissionDirection>("direction")
    val documentType = dbEnumeration<PeppolDocumentType>("document_type")
    val status = dbEnumeration<PeppolStatus>("status").default(PeppolStatus.Pending)

    // Local document references
    val invoiceId = uuid("invoice_id").references(
        ai.dokus.foundation.database.tables.cashflow.InvoicesTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable().index()  // For outbound
    val billId = uuid("bill_id").references(
        ai.dokus.foundation.database.tables.cashflow.BillsTable.id,
        onDelete = ReferenceOption.SET_NULL
    ).nullable().index()  // For inbound

    // External references
    val externalDocumentId = varchar("external_document_id", 255).nullable().index()

    // Peppol IDs
    val recipientPeppolId = varchar("recipient_peppol_id", 255).nullable()  // For outbound
    val senderPeppolId = varchar("sender_peppol_id", 255).nullable()  // For inbound

    // Error tracking
    val errorMessage = text("error_message").nullable()

    // Raw data for debugging/audit
    val rawRequest = text("raw_request").nullable()
    val rawResponse = text("raw_response").nullable()

    // Timestamps
    val transmittedAt = datetime("transmitted_at").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite index for common queries
        index(false, tenantId, direction, status)
        uniqueIndex(tenantId, externalDocumentId)
    }
}
