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
// VatReturn
// ═══════════════════════════════════════════════════════════════════

object VatReturnDraftsTable : UUIDTable("vat_return_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_return_drafts_document", tenantId, documentId)
    }
}

object VatReturnConfirmedTable : UUIDTable("vat_return_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_return_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// VatListing
// ═══════════════════════════════════════════════════════════════════

object VatListingDraftsTable : UUIDTable("vat_listing_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_listing_drafts_document", tenantId, documentId)
    }
}

object VatListingConfirmedTable : UUIDTable("vat_listing_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_listing_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// VatAssessment
// ═══════════════════════════════════════════════════════════════════

object VatAssessmentDraftsTable : UUIDTable("vat_assessment_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_assessment_drafts_document", tenantId, documentId)
    }
}

object VatAssessmentConfirmedTable : UUIDTable("vat_assessment_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_vat_assessment_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// IcListing
// ═══════════════════════════════════════════════════════════════════

object IcListingDraftsTable : UUIDTable("ic_listing_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_ic_listing_drafts_document", tenantId, documentId)
    }
}

object IcListingConfirmedTable : UUIDTable("ic_listing_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_ic_listing_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// OssReturn
// ═══════════════════════════════════════════════════════════════════

object OssReturnDraftsTable : UUIDTable("oss_return_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_oss_return_drafts_document", tenantId, documentId)
    }
}

object OssReturnConfirmedTable : UUIDTable("oss_return_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_oss_return_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// CorporateTax
// ═══════════════════════════════════════════════════════════════════

object CorporateTaxDraftsTable : UUIDTable("corporate_tax_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_corporate_tax_drafts_document", tenantId, documentId)
    }
}

object CorporateTaxConfirmedTable : UUIDTable("corporate_tax_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_corporate_tax_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// CorporateTaxAdvance
// ═══════════════════════════════════════════════════════════════════

object CorporateTaxAdvanceDraftsTable : UUIDTable("corporate_tax_advance_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_corporate_tax_advance_drafts_document", tenantId, documentId)
    }
}

object CorporateTaxAdvanceConfirmedTable : UUIDTable("corporate_tax_advance_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_corporate_tax_advance_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// TaxAssessment
// ═══════════════════════════════════════════════════════════════════

object TaxAssessmentDraftsTable : UUIDTable("tax_assessment_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_tax_assessment_drafts_document", tenantId, documentId)
    }
}

object TaxAssessmentConfirmedTable : UUIDTable("tax_assessment_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_tax_assessment_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// PersonalTax
// ═══════════════════════════════════════════════════════════════════

object PersonalTaxDraftsTable : UUIDTable("personal_tax_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_personal_tax_drafts_document", tenantId, documentId)
    }
}

object PersonalTaxConfirmedTable : UUIDTable("personal_tax_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_personal_tax_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// WithholdingTax
// ═══════════════════════════════════════════════════════════════════

object WithholdingTaxDraftsTable : UUIDTable("withholding_tax_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_withholding_tax_drafts_document", tenantId, documentId)
    }
}

object WithholdingTaxConfirmedTable : UUIDTable("withholding_tax_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_withholding_tax_confirmed_document", tenantId, documentId)
    }
}
