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
// SocialContribution
// ═══════════════════════════════════════════════════════════════════

object SocialContributionDraftsTable : UUIDTable("social_contribution_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_social_contribution_drafts_document", tenantId, documentId)
    }
}

object SocialContributionConfirmedTable : UUIDTable("social_contribution_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_social_contribution_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// SocialFund
// ═══════════════════════════════════════════════════════════════════

object SocialFundDraftsTable : UUIDTable("social_fund_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_social_fund_drafts_document", tenantId, documentId)
    }
}

object SocialFundConfirmedTable : UUIDTable("social_fund_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_social_fund_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// SelfEmployedContribution
// ═══════════════════════════════════════════════════════════════════

object SelfEmployedContributionDraftsTable : UUIDTable("self_employed_contribution_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_self_employed_contribution_drafts_document", tenantId, documentId)
    }
}

object SelfEmployedContributionConfirmedTable : UUIDTable("self_employed_contribution_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_self_employed_contribution_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Vapz
// ═══════════════════════════════════════════════════════════════════

object VapzDraftsTable : UUIDTable("vapz_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vapz_drafts_document", tenantId, documentId)
    }
}

object VapzConfirmedTable : UUIDTable("vapz_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vapz_confirmed_document", tenantId, documentId)
    }
}
