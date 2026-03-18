package tech.dokus.database.tables.drafts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.foundation.backend.database.dbEnumeration

// ═══════════════════════════════════════════════════════════════════
// ProForma
// ═══════════════════════════════════════════════════════════════════

object ProFormaDraftsTable : UUIDTable("pro_forma_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_pro_forma_drafts_document", tenantId, documentId)
    }
}

object ProFormaConfirmedTable : UUIDTable("pro_forma_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_pro_forma_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Quote
// ═══════════════════════════════════════════════════════════════════

object QuoteDraftsTable : UUIDTable("quote_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_quote_drafts_document", tenantId, documentId)
    }
}

object QuoteConfirmedTable : UUIDTable("quote_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_quote_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// OrderConfirmation
// ═══════════════════════════════════════════════════════════════════

object OrderConfirmationDraftsTable : UUIDTable("order_confirmation_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_order_confirmation_drafts_document", tenantId, documentId)
    }
}

object OrderConfirmationConfirmedTable : UUIDTable("order_confirmation_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_order_confirmation_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// DeliveryNote
// ═══════════════════════════════════════════════════════════════════

object DeliveryNoteDraftsTable : UUIDTable("delivery_note_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_delivery_note_drafts_document", tenantId, documentId)
    }
}

object DeliveryNoteConfirmedTable : UUIDTable("delivery_note_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_delivery_note_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Reminder
// ═══════════════════════════════════════════════════════════════════

object ReminderDraftsTable : UUIDTable("reminder_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_reminder_drafts_document", tenantId, documentId)
    }
}

object ReminderConfirmedTable : UUIDTable("reminder_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_reminder_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// StatementOfAccount
// ═══════════════════════════════════════════════════════════════════

object StatementOfAccountDraftsTable : UUIDTable("statement_of_account_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_statement_of_account_drafts_document", tenantId, documentId)
    }
}

object StatementOfAccountConfirmedTable : UUIDTable("statement_of_account_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_statement_of_account_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// PurchaseOrder
// ═══════════════════════════════════════════════════════════════════

object PurchaseOrderDraftsTable : UUIDTable("purchase_order_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_purchase_order_drafts_document", tenantId, documentId)
    }
}

object PurchaseOrderConfirmedTable : UUIDTable("purchase_order_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_purchase_order_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// ExpenseClaim
// ═══════════════════════════════════════════════════════════════════

object ExpenseClaimDraftsTable : UUIDTable("expense_claim_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_expense_claim_drafts_document", tenantId, documentId)
    }
}

object ExpenseClaimConfirmedTable : UUIDTable("expense_claim_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_expense_claim_confirmed_document", tenantId, documentId)
    }
}
