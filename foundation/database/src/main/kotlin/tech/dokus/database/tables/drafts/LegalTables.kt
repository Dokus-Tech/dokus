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
// Contract
// ═══════════════════════════════════════════════════════════════════

object ContractDraftsTable : UUIDTable("contract_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_contract_drafts_document", tenantId, documentId)
    }
}

object ContractConfirmedTable : UUIDTable("contract_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_contract_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Lease
// ═══════════════════════════════════════════════════════════════════

object LeaseDraftsTable : UUIDTable("lease_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_lease_drafts_document", tenantId, documentId)
    }
}

object LeaseConfirmedTable : UUIDTable("lease_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_lease_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Loan
// ═══════════════════════════════════════════════════════════════════

object LoanDraftsTable : UUIDTable("loan_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_loan_drafts_document", tenantId, documentId)
    }
}

object LoanConfirmedTable : UUIDTable("loan_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_loan_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Insurance
// ═══════════════════════════════════════════════════════════════════

object InsuranceDraftsTable : UUIDTable("insurance_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_insurance_drafts_document", tenantId, documentId)
    }
}

object InsuranceConfirmedTable : UUIDTable("insurance_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_insurance_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// Dividend
// ═══════════════════════════════════════════════════════════════════

object DividendDraftsTable : UUIDTable("dividend_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_dividend_drafts_document", tenantId, documentId)
    }
}

object DividendConfirmedTable : UUIDTable("dividend_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_dividend_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// ShareholderRegister
// ═══════════════════════════════════════════════════════════════════

object ShareholderRegisterDraftsTable : UUIDTable("shareholder_register_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_shareholder_register_drafts_document", tenantId, documentId)
    }
}

object ShareholderRegisterConfirmedTable : UUIDTable("shareholder_register_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_shareholder_register_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// CompanyExtract
// ═══════════════════════════════════════════════════════════════════

object CompanyExtractDraftsTable : UUIDTable("company_extract_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_company_extract_drafts_document", tenantId, documentId)
    }
}

object CompanyExtractConfirmedTable : UUIDTable("company_extract_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_company_extract_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// AnnualAccounts
// ═══════════════════════════════════════════════════════════════════

object AnnualAccountsDraftsTable : UUIDTable("annual_accounts_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_annual_accounts_drafts_document", tenantId, documentId)
    }
}

object AnnualAccountsConfirmedTable : UUIDTable("annual_accounts_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_annual_accounts_confirmed_document", tenantId, documentId)
    }
}

// ═══════════════════════════════════════════════════════════════════
// BoardMinutes
// ═══════════════════════════════════════════════════════════════════

object BoardMinutesDraftsTable : UUIDTable("board_minutes_drafts") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_board_minutes_drafts_document", tenantId, documentId)
    }
}

object BoardMinutesConfirmedTable : UUIDTable("board_minutes_confirmed") {
    val tenantId = uuid("tenant_id").references(TenantTable.id, onDelete = ReferenceOption.CASCADE).index()
    val documentId = uuid("document_id").references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
    val direction = dbEnumeration<DocumentDirection>("direction").default(DocumentDirection.Unknown)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex("ux_board_minutes_confirmed_document", tenantId, documentId)
    }
}
