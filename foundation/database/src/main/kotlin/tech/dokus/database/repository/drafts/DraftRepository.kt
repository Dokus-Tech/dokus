@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@file:Suppress("TooManyFunctions", "CyclomaticComplexity")

package tech.dokus.database.repository.drafts

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.entity.*
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.drafts.*
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.toDbDecimal
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.*
import tech.dokus.foundation.backend.database.dbQuery
import kotlin.uuid.toJavaUuid

/**
 * Repository for reading document drafts from per-type draft tables.
 * Dispatches to the correct table based on [DocumentType].
 */
class DraftRepository {

    /**
     * Load the draft for a document as [DocDto].
     * Returns null if no draft exists for this document.
     */
    suspend fun getDraftAsDocDto(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType?,
    ): DocDto? {
        if (documentType == null) return null
        return when (documentType) {
            DocumentType.Invoice -> loadInvoiceDraft(tenantId, documentId)
            DocumentType.CreditNote -> loadCreditNoteDraft(tenantId, documentId)
            DocumentType.Receipt -> loadReceiptDraft(tenantId, documentId)
            DocumentType.BankStatement -> loadBankStatementDraft(tenantId, documentId)
            DocumentType.ProForma -> loadClassifiedDraft(tenantId, documentId, ProFormaDraftsTable) { ProFormaDraftEntity.from(it) }.let { it?.let { e -> DocDto.ProForma.Draft(e.direction) } }
            DocumentType.Quote -> loadClassifiedDraft(tenantId, documentId, QuoteDraftsTable) { QuoteDraftEntity.from(it) }.let { it?.let { e -> DocDto.Quote.Draft(e.direction) } }
            DocumentType.OrderConfirmation -> loadClassifiedDraft(tenantId, documentId, OrderConfirmationDraftsTable) { OrderConfirmationDraftEntity.from(it) }.let { it?.let { e -> DocDto.OrderConfirmation.Draft(e.direction) } }
            DocumentType.DeliveryNote -> loadClassifiedDraft(tenantId, documentId, DeliveryNoteDraftsTable) { DeliveryNoteDraftEntity.from(it) }.let { it?.let { e -> DocDto.DeliveryNote.Draft(e.direction) } }
            DocumentType.Reminder -> loadClassifiedDraft(tenantId, documentId, ReminderDraftsTable) { ReminderDraftEntity.from(it) }.let { it?.let { e -> DocDto.Reminder.Draft(e.direction) } }
            DocumentType.StatementOfAccount -> loadClassifiedDraft(tenantId, documentId, StatementOfAccountDraftsTable) { StatementOfAccountDraftEntity.from(it) }.let { it?.let { e -> DocDto.StatementOfAccount.Draft(e.direction) } }
            DocumentType.PurchaseOrder -> loadClassifiedDraft(tenantId, documentId, PurchaseOrderDraftsTable) { PurchaseOrderDraftEntity.from(it) }.let { it?.let { e -> DocDto.PurchaseOrder.Draft(e.direction) } }
            DocumentType.ExpenseClaim -> loadClassifiedDraft(tenantId, documentId, ExpenseClaimDraftsTable) { ExpenseClaimDraftEntity.from(it) }.let { it?.let { e -> DocDto.ExpenseClaim.Draft(e.direction) } }
            DocumentType.BankFee -> loadClassifiedDraft(tenantId, documentId, BankFeeDraftsTable) { BankFeeDraftEntity.from(it) }.let { it?.let { e -> DocDto.BankFee.Draft(e.direction) } }
            DocumentType.InterestStatement -> loadClassifiedDraft(tenantId, documentId, InterestStatementDraftsTable) { InterestStatementDraftEntity.from(it) }.let { it?.let { e -> DocDto.InterestStatement.Draft(e.direction) } }
            DocumentType.PaymentConfirmation -> loadClassifiedDraft(tenantId, documentId, PaymentConfirmationDraftsTable) { PaymentConfirmationDraftEntity.from(it) }.let { it?.let { e -> DocDto.PaymentConfirmation.Draft(e.direction) } }
            DocumentType.VatReturn -> loadClassifiedDraft(tenantId, documentId, VatReturnDraftsTable) { VatReturnDraftEntity.from(it) }.let { it?.let { e -> DocDto.VatReturn.Draft(e.direction) } }
            DocumentType.VatListing -> loadClassifiedDraft(tenantId, documentId, VatListingDraftsTable) { VatListingDraftEntity.from(it) }.let { it?.let { e -> DocDto.VatListing.Draft(e.direction) } }
            DocumentType.VatAssessment -> loadClassifiedDraft(tenantId, documentId, VatAssessmentDraftsTable) { VatAssessmentDraftEntity.from(it) }.let { it?.let { e -> DocDto.VatAssessment.Draft(e.direction) } }
            DocumentType.IcListing -> loadClassifiedDraft(tenantId, documentId, IcListingDraftsTable) { IcListingDraftEntity.from(it) }.let { it?.let { e -> DocDto.IcListing.Draft(e.direction) } }
            DocumentType.OssReturn -> loadClassifiedDraft(tenantId, documentId, OssReturnDraftsTable) { OssReturnDraftEntity.from(it) }.let { it?.let { e -> DocDto.OssReturn.Draft(e.direction) } }
            DocumentType.CorporateTax -> loadClassifiedDraft(tenantId, documentId, CorporateTaxDraftsTable) { CorporateTaxDraftEntity.from(it) }.let { it?.let { e -> DocDto.CorporateTax.Draft(e.direction) } }
            DocumentType.CorporateTaxAdvance -> loadClassifiedDraft(tenantId, documentId, CorporateTaxAdvanceDraftsTable) { CorporateTaxAdvanceDraftEntity.from(it) }.let { it?.let { e -> DocDto.CorporateTaxAdvance.Draft(e.direction) } }
            DocumentType.TaxAssessment -> loadClassifiedDraft(tenantId, documentId, TaxAssessmentDraftsTable) { TaxAssessmentDraftEntity.from(it) }.let { it?.let { e -> DocDto.TaxAssessment.Draft(e.direction) } }
            DocumentType.PersonalTax -> loadClassifiedDraft(tenantId, documentId, PersonalTaxDraftsTable) { PersonalTaxDraftEntity.from(it) }.let { it?.let { e -> DocDto.PersonalTax.Draft(e.direction) } }
            DocumentType.WithholdingTax -> loadClassifiedDraft(tenantId, documentId, WithholdingTaxDraftsTable) { WithholdingTaxDraftEntity.from(it) }.let { it?.let { e -> DocDto.WithholdingTax.Draft(e.direction) } }
            DocumentType.SocialContribution -> loadClassifiedDraft(tenantId, documentId, SocialContributionDraftsTable) { SocialContributionDraftEntity.from(it) }.let { it?.let { e -> DocDto.SocialContribution.Draft(e.direction) } }
            DocumentType.SocialFund -> loadClassifiedDraft(tenantId, documentId, SocialFundDraftsTable) { SocialFundDraftEntity.from(it) }.let { it?.let { e -> DocDto.SocialFund.Draft(e.direction) } }
            DocumentType.SelfEmployedContribution -> loadClassifiedDraft(tenantId, documentId, SelfEmployedContributionDraftsTable) { SelfEmployedContributionDraftEntity.from(it) }.let { it?.let { e -> DocDto.SelfEmployedContribution.Draft(e.direction) } }
            DocumentType.Vapz -> loadClassifiedDraft(tenantId, documentId, VapzDraftsTable) { VapzDraftEntity.from(it) }.let { it?.let { e -> DocDto.Vapz.Draft(e.direction) } }
            DocumentType.SalarySlip -> loadClassifiedDraft(tenantId, documentId, SalarySlipDraftsTable) { SalarySlipDraftEntity.from(it) }.let { it?.let { e -> DocDto.SalarySlip.Draft(e.direction) } }
            DocumentType.PayrollSummary -> loadClassifiedDraft(tenantId, documentId, PayrollSummaryDraftsTable) { PayrollSummaryDraftEntity.from(it) }.let { it?.let { e -> DocDto.PayrollSummary.Draft(e.direction) } }
            DocumentType.EmploymentContract -> loadClassifiedDraft(tenantId, documentId, EmploymentContractDraftsTable) { EmploymentContractDraftEntity.from(it) }.let { it?.let { e -> DocDto.EmploymentContract.Draft(e.direction) } }
            DocumentType.Dimona -> loadClassifiedDraft(tenantId, documentId, DimonaDraftsTable) { DimonaDraftEntity.from(it) }.let { it?.let { e -> DocDto.Dimona.Draft(e.direction) } }
            DocumentType.C4 -> loadClassifiedDraft(tenantId, documentId, C4DraftsTable) { C4DraftEntity.from(it) }.let { it?.let { e -> DocDto.C4.Draft(e.direction) } }
            DocumentType.HolidayPay -> loadClassifiedDraft(tenantId, documentId, HolidayPayDraftsTable) { HolidayPayDraftEntity.from(it) }.let { it?.let { e -> DocDto.HolidayPay.Draft(e.direction) } }
            DocumentType.Contract -> loadClassifiedDraft(tenantId, documentId, ContractDraftsTable) { ContractDraftEntity.from(it) }.let { it?.let { e -> DocDto.Contract.Draft(e.direction) } }
            DocumentType.Lease -> loadClassifiedDraft(tenantId, documentId, LeaseDraftsTable) { LeaseDraftEntity.from(it) }.let { it?.let { e -> DocDto.Lease.Draft(e.direction) } }
            DocumentType.Loan -> loadClassifiedDraft(tenantId, documentId, LoanDraftsTable) { LoanDraftEntity.from(it) }.let { it?.let { e -> DocDto.Loan.Draft(e.direction) } }
            DocumentType.Insurance -> loadClassifiedDraft(tenantId, documentId, InsuranceDraftsTable) { InsuranceDraftEntity.from(it) }.let { it?.let { e -> DocDto.Insurance.Draft(e.direction) } }
            DocumentType.Dividend -> loadClassifiedDraft(tenantId, documentId, DividendDraftsTable) { DividendDraftEntity.from(it) }.let { it?.let { e -> DocDto.Dividend.Draft(e.direction) } }
            DocumentType.ShareholderRegister -> loadClassifiedDraft(tenantId, documentId, ShareholderRegisterDraftsTable) { ShareholderRegisterDraftEntity.from(it) }.let { it?.let { e -> DocDto.ShareholderRegister.Draft(e.direction) } }
            DocumentType.CompanyExtract -> loadClassifiedDraft(tenantId, documentId, CompanyExtractDraftsTable) { CompanyExtractDraftEntity.from(it) }.let { it?.let { e -> DocDto.CompanyExtract.Draft(e.direction) } }
            DocumentType.AnnualAccounts -> loadClassifiedDraft(tenantId, documentId, AnnualAccountsDraftsTable) { AnnualAccountsDraftEntity.from(it) }.let { it?.let { e -> DocDto.AnnualAccounts.Draft(e.direction) } }
            DocumentType.BoardMinutes -> loadClassifiedDraft(tenantId, documentId, BoardMinutesDraftsTable) { BoardMinutesDraftEntity.from(it) }.let { it?.let { e -> DocDto.BoardMinutes.Draft(e.direction) } }
            DocumentType.Subsidy -> loadClassifiedDraft(tenantId, documentId, SubsidyDraftsTable) { SubsidyDraftEntity.from(it) }.let { it?.let { e -> DocDto.Subsidy.Draft(e.direction) } }
            DocumentType.Fine -> loadClassifiedDraft(tenantId, documentId, FineDraftsTable) { FineDraftEntity.from(it) }.let { it?.let { e -> DocDto.Fine.Draft(e.direction) } }
            DocumentType.Permit -> loadClassifiedDraft(tenantId, documentId, PermitDraftsTable) { PermitDraftEntity.from(it) }.let { it?.let { e -> DocDto.Permit.Draft(e.direction) } }
            DocumentType.CustomsDeclaration -> loadClassifiedDraft(tenantId, documentId, CustomsDeclarationDraftsTable) { CustomsDeclarationDraftEntity.from(it) }.let { it?.let { e -> DocDto.CustomsDeclaration.Draft(e.direction) } }
            DocumentType.Intrastat -> loadClassifiedDraft(tenantId, documentId, IntrastatDraftsTable) { IntrastatDraftEntity.from(it) }.let { it?.let { e -> DocDto.Intrastat.Draft(e.direction) } }
            DocumentType.DepreciationSchedule -> loadClassifiedDraft(tenantId, documentId, DepreciationScheduleDraftsTable) { DepreciationScheduleDraftEntity.from(it) }.let { it?.let { e -> DocDto.DepreciationSchedule.Draft(e.direction) } }
            DocumentType.Inventory -> loadClassifiedDraft(tenantId, documentId, InventoryDraftsTable) { InventoryDraftEntity.from(it) }.let { it?.let { e -> DocDto.Inventory.Draft(e.direction) } }
            DocumentType.Other -> loadClassifiedDraft(tenantId, documentId, OtherDraftsTable) { OtherDraftEntity.from(it) }.let { it?.let { e -> DocDto.Other.Draft(e.direction) } }
            DocumentType.Unknown -> null
        }
    }

    // ==========================================================================
    // Core financial draft loaders
    // ==========================================================================

    private suspend fun loadInvoiceDraft(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocDto.Invoice.Draft? = dbQuery {
        val row = InvoiceDraftsTable.selectAll().where {
            (InvoiceDraftsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoiceDraftsTable.documentId eq documentId.value.toJavaUuid())
        }.singleOrNull() ?: return@dbQuery null

        val draftId = row[InvoiceDraftsTable.id].value
        val itemRows = InvoiceDraftItemsTable.selectAll().where {
            InvoiceDraftItemsTable.draftId eq draftId
        }.orderBy(InvoiceDraftItemsTable.sortOrder).toList()

        val items = itemRows.map { InvoiceDraftItemEntity.from(it) }
        val entity = InvoiceDraftEntity.from(row, items)
        entity.toDocDto()
    }

    private suspend fun loadCreditNoteDraft(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocDto.CreditNote.Draft? = dbQuery {
        val row = CreditNoteDraftsTable.selectAll().where {
            (CreditNoteDraftsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (CreditNoteDraftsTable.documentId eq documentId.value.toJavaUuid())
        }.singleOrNull() ?: return@dbQuery null

        val draftId = row[CreditNoteDraftsTable.id].value
        val itemRows = CreditNoteDraftItemsTable.selectAll().where {
            CreditNoteDraftItemsTable.draftId eq draftId
        }.orderBy(CreditNoteDraftItemsTable.sortOrder).toList()

        val items = itemRows.map { CreditNoteDraftItemEntity.from(it) }
        val entity = CreditNoteDraftEntity.from(row, items)
        entity.toDocDto()
    }

    private suspend fun loadReceiptDraft(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocDto.Receipt.Draft? = dbQuery {
        val row = ReceiptDraftsTable.selectAll().where {
            (ReceiptDraftsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ReceiptDraftsTable.documentId eq documentId.value.toJavaUuid())
        }.singleOrNull() ?: return@dbQuery null

        val entity = ReceiptDraftEntity.from(row)
        entity.toDocDto()
    }

    private suspend fun loadBankStatementDraft(
        tenantId: TenantId,
        documentId: DocumentId,
    ): DocDto.BankStatement.Draft? = dbQuery {
        val row = BankStatementDraftsTable.selectAll().where {
            (BankStatementDraftsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementDraftsTable.documentId eq documentId.value.toJavaUuid())
        }.singleOrNull() ?: return@dbQuery null

        val entity = BankStatementDraftEntity.from(row)
        entity.toDocDto()
    }

    // ==========================================================================
    // Classified draft loader (generic helper)
    // ==========================================================================

    /**
     * Generic helper to load a classified draft from any table that follows
     * the standard classified schema (id, tenant_id, document_id, direction, ...).
     */
    private suspend fun <T> loadClassifiedDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        table: UUIDTable,
        mapper: (ResultRow) -> T,
    ): T? = dbQuery {
        val tenantCol = table.columns.first { it.name == "tenant_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>
        val docCol = table.columns.first { it.name == "document_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>

        val row = table.selectAll().where {
            (tenantCol eq tenantId.value.toJavaUuid()) and
                (docCol eq documentId.value.toJavaUuid())
        }.singleOrNull() ?: return@dbQuery null

        mapper(row)
    }

    /**
     * Delete a draft for a document (used during confirmation or type change).
     */
    suspend fun deleteDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
    ): Boolean = dbQuery {
        val table = draftTableFor(documentType) ?: return@dbQuery false
        val tenantCol = table.columns.first { it.name == "tenant_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>
        val docCol = table.columns.first { it.name == "document_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>

        val deleted = table.deleteWhere {
            (tenantCol eq tenantId.value.toJavaUuid()) and
                (docCol eq documentId.value.toJavaUuid())
        }
        deleted > 0
    }

    /**
     * Save extracted draft data to the appropriate per-type draft table.
     * Upserts: deletes existing draft for this document, then inserts new one.
     * Called by the AI extraction pipeline after successful extraction.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    suspend fun saveDraftFromExtraction(
        tenantId: TenantId,
        documentId: DocumentId,
        extractedData: DocumentDraftData,
    ): Boolean {
        val docType = extractedData.toDocumentType()
        deleteDraft(tenantId, documentId, docType)

        return when (extractedData) {
            is InvoiceDraftData -> saveInvoiceDraft(tenantId, documentId, extractedData)
            is CreditNoteDraftData -> saveCreditNoteDraft(tenantId, documentId, extractedData)
            is ReceiptDraftData -> saveReceiptDraft(tenantId, documentId, extractedData)
            is BankStatementDraftData -> saveBankStatementDraft(tenantId, documentId, extractedData)
            is ProFormaDraftData -> insertClassifiedDraft(tenantId, documentId, ProFormaDraftsTable, extractedData.direction)
            is QuoteDraftData -> insertClassifiedDraft(tenantId, documentId, QuoteDraftsTable, extractedData.direction)
            is OrderConfirmationDraftData -> insertClassifiedDraft(tenantId, documentId, OrderConfirmationDraftsTable, extractedData.direction)
            is DeliveryNoteDraftData -> insertClassifiedDraft(tenantId, documentId, DeliveryNoteDraftsTable, extractedData.direction)
            is ReminderDraftData -> insertClassifiedDraft(tenantId, documentId, ReminderDraftsTable, extractedData.direction)
            is StatementOfAccountDraftData -> insertClassifiedDraft(tenantId, documentId, StatementOfAccountDraftsTable, extractedData.direction)
            is PurchaseOrderDraftData -> insertClassifiedDraft(tenantId, documentId, PurchaseOrderDraftsTable, extractedData.direction)
            is ExpenseClaimDraftData -> insertClassifiedDraft(tenantId, documentId, ExpenseClaimDraftsTable, extractedData.direction)
            is BankFeeDraftData -> insertClassifiedDraft(tenantId, documentId, BankFeeDraftsTable, extractedData.direction)
            is InterestStatementDraftData -> insertClassifiedDraft(tenantId, documentId, InterestStatementDraftsTable, extractedData.direction)
            is PaymentConfirmationDraftData -> insertClassifiedDraft(tenantId, documentId, PaymentConfirmationDraftsTable, extractedData.direction)
            is VatReturnDraftData -> insertClassifiedDraft(tenantId, documentId, VatReturnDraftsTable, extractedData.direction)
            is VatListingDraftData -> insertClassifiedDraft(tenantId, documentId, VatListingDraftsTable, extractedData.direction)
            is VatAssessmentDraftData -> insertClassifiedDraft(tenantId, documentId, VatAssessmentDraftsTable, extractedData.direction)
            is IcListingDraftData -> insertClassifiedDraft(tenantId, documentId, IcListingDraftsTable, extractedData.direction)
            is OssReturnDraftData -> insertClassifiedDraft(tenantId, documentId, OssReturnDraftsTable, extractedData.direction)
            is CorporateTaxDraftData -> insertClassifiedDraft(tenantId, documentId, CorporateTaxDraftsTable, extractedData.direction)
            is CorporateTaxAdvanceDraftData -> insertClassifiedDraft(tenantId, documentId, CorporateTaxAdvanceDraftsTable, extractedData.direction)
            is TaxAssessmentDraftData -> insertClassifiedDraft(tenantId, documentId, TaxAssessmentDraftsTable, extractedData.direction)
            is PersonalTaxDraftData -> insertClassifiedDraft(tenantId, documentId, PersonalTaxDraftsTable, extractedData.direction)
            is WithholdingTaxDraftData -> insertClassifiedDraft(tenantId, documentId, WithholdingTaxDraftsTable, extractedData.direction)
            is SocialContributionDraftData -> insertClassifiedDraft(tenantId, documentId, SocialContributionDraftsTable, extractedData.direction)
            is SocialFundDraftData -> insertClassifiedDraft(tenantId, documentId, SocialFundDraftsTable, extractedData.direction)
            is SelfEmployedContributionDraftData -> insertClassifiedDraft(tenantId, documentId, SelfEmployedContributionDraftsTable, extractedData.direction)
            is VapzDraftData -> insertClassifiedDraft(tenantId, documentId, VapzDraftsTable, extractedData.direction)
            is SalarySlipDraftData -> insertClassifiedDraft(tenantId, documentId, SalarySlipDraftsTable, extractedData.direction)
            is PayrollSummaryDraftData -> insertClassifiedDraft(tenantId, documentId, PayrollSummaryDraftsTable, extractedData.direction)
            is EmploymentContractDraftData -> insertClassifiedDraft(tenantId, documentId, EmploymentContractDraftsTable, extractedData.direction)
            is DimonaDraftData -> insertClassifiedDraft(tenantId, documentId, DimonaDraftsTable, extractedData.direction)
            is C4DraftData -> insertClassifiedDraft(tenantId, documentId, C4DraftsTable, extractedData.direction)
            is HolidayPayDraftData -> insertClassifiedDraft(tenantId, documentId, HolidayPayDraftsTable, extractedData.direction)
            is ContractDraftData -> insertClassifiedDraft(tenantId, documentId, ContractDraftsTable, extractedData.direction)
            is LeaseDraftData -> insertClassifiedDraft(tenantId, documentId, LeaseDraftsTable, extractedData.direction)
            is LoanDraftData -> insertClassifiedDraft(tenantId, documentId, LoanDraftsTable, extractedData.direction)
            is InsuranceDraftData -> insertClassifiedDraft(tenantId, documentId, InsuranceDraftsTable, extractedData.direction)
            is DividendDraftData -> insertClassifiedDraft(tenantId, documentId, DividendDraftsTable, extractedData.direction)
            is ShareholderRegisterDraftData -> insertClassifiedDraft(tenantId, documentId, ShareholderRegisterDraftsTable, extractedData.direction)
            is CompanyExtractDraftData -> insertClassifiedDraft(tenantId, documentId, CompanyExtractDraftsTable, extractedData.direction)
            is AnnualAccountsDraftData -> insertClassifiedDraft(tenantId, documentId, AnnualAccountsDraftsTable, extractedData.direction)
            is BoardMinutesDraftData -> insertClassifiedDraft(tenantId, documentId, BoardMinutesDraftsTable, extractedData.direction)
            is SubsidyDraftData -> insertClassifiedDraft(tenantId, documentId, SubsidyDraftsTable, extractedData.direction)
            is FineDraftData -> insertClassifiedDraft(tenantId, documentId, FineDraftsTable, extractedData.direction)
            is PermitDraftData -> insertClassifiedDraft(tenantId, documentId, PermitDraftsTable, extractedData.direction)
            is CustomsDeclarationDraftData -> insertClassifiedDraft(tenantId, documentId, CustomsDeclarationDraftsTable, extractedData.direction)
            is IntrastatDraftData -> insertClassifiedDraft(tenantId, documentId, IntrastatDraftsTable, extractedData.direction)
            is DepreciationScheduleDraftData -> insertClassifiedDraft(tenantId, documentId, DepreciationScheduleDraftsTable, extractedData.direction)
            is InventoryDraftData -> insertClassifiedDraft(tenantId, documentId, InventoryDraftsTable, extractedData.direction)
            is OtherDraftData -> insertClassifiedDraft(tenantId, documentId, OtherDraftsTable, extractedData.direction)
        }
    }

    // ==========================================================================
    // Core financial draft writers
    // ==========================================================================

    private suspend fun saveInvoiceDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        data: InvoiceDraftData,
    ): Boolean = dbQuery {
        val draftId = InvoiceDraftsTable.insertAndGetId {
            it[InvoiceDraftsTable.tenantId] = tenantId.value.toJavaUuid()
            it[InvoiceDraftsTable.documentId] = documentId.value.toJavaUuid()
            it[InvoiceDraftsTable.invoiceNumber] = data.invoiceNumber
            it[InvoiceDraftsTable.issueDate] = data.issueDate
            it[InvoiceDraftsTable.dueDate] = data.dueDate
            it[InvoiceDraftsTable.direction] = data.direction
            it[InvoiceDraftsTable.currency] = data.currency
            it[InvoiceDraftsTable.subtotalAmount] = data.subtotalAmount?.toDbDecimal()
            it[InvoiceDraftsTable.vatAmount] = data.vatAmount?.toDbDecimal()
            it[InvoiceDraftsTable.totalAmount] = data.totalAmount?.toDbDecimal()
            it[InvoiceDraftsTable.notes] = data.notes
            it[InvoiceDraftsTable.senderIban] = data.iban?.value
            it[InvoiceDraftsTable.structuredCommunication] = data.payment?.structuredComm?.value
        }
        data.lineItems.forEachIndexed { index, item ->
            InvoiceDraftItemsTable.insert {
                it[InvoiceDraftItemsTable.draftId] = draftId.value
                it[InvoiceDraftItemsTable.description] = item.description
                it[InvoiceDraftItemsTable.quantity] = item.quantity?.toBigDecimal()
                it[InvoiceDraftItemsTable.unitPrice] = item.unitPrice?.let { price -> Money(price).toDbDecimal() }
                it[InvoiceDraftItemsTable.vatRate] = item.vatRate?.let { rate -> VatRate(rate).toDbDecimal() }
                it[InvoiceDraftItemsTable.lineTotal] = item.netAmount?.let { amount -> Money(amount).toDbDecimal() }
                it[InvoiceDraftItemsTable.vatAmount] = null
                it[InvoiceDraftItemsTable.sortOrder] = index
            }
        }
        true
    }

    private suspend fun saveCreditNoteDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        data: CreditNoteDraftData,
    ): Boolean = dbQuery {
        val draftId = CreditNoteDraftsTable.insertAndGetId {
            it[CreditNoteDraftsTable.tenantId] = tenantId.value.toJavaUuid()
            it[CreditNoteDraftsTable.documentId] = documentId.value.toJavaUuid()
            it[CreditNoteDraftsTable.creditNoteNumber] = data.creditNoteNumber
            it[CreditNoteDraftsTable.issueDate] = data.issueDate
            it[CreditNoteDraftsTable.direction] = data.direction
            it[CreditNoteDraftsTable.currency] = data.currency
            it[CreditNoteDraftsTable.subtotalAmount] = data.subtotalAmount?.toDbDecimal()
            it[CreditNoteDraftsTable.vatAmount] = data.vatAmount?.toDbDecimal()
            it[CreditNoteDraftsTable.totalAmount] = data.totalAmount?.toDbDecimal()
            it[CreditNoteDraftsTable.originalInvoiceNumber] = data.originalInvoiceNumber
            it[CreditNoteDraftsTable.reason] = data.reason
            it[CreditNoteDraftsTable.notes] = data.notes
        }
        data.lineItems.forEachIndexed { index, item ->
            CreditNoteDraftItemsTable.insert {
                it[CreditNoteDraftItemsTable.draftId] = draftId.value
                it[CreditNoteDraftItemsTable.description] = item.description
                it[CreditNoteDraftItemsTable.quantity] = item.quantity?.toBigDecimal()
                it[CreditNoteDraftItemsTable.unitPrice] = item.unitPrice?.let { price -> Money(price).toDbDecimal() }
                it[CreditNoteDraftItemsTable.vatRate] = item.vatRate?.let { rate -> VatRate(rate).toDbDecimal() }
                it[CreditNoteDraftItemsTable.lineTotal] = item.netAmount?.let { amount -> Money(amount).toDbDecimal() }
                it[CreditNoteDraftItemsTable.vatAmount] = null
                it[CreditNoteDraftItemsTable.sortOrder] = index
            }
        }
        true
    }

    private suspend fun saveReceiptDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        data: ReceiptDraftData,
    ): Boolean = dbQuery {
        ReceiptDraftsTable.insert {
            it[ReceiptDraftsTable.tenantId] = tenantId.value.toJavaUuid()
            it[ReceiptDraftsTable.documentId] = documentId.value.toJavaUuid()
            it[ReceiptDraftsTable.merchantName] = data.merchantName
            it[ReceiptDraftsTable.merchantVat] = data.merchantVat?.value
            it[ReceiptDraftsTable.date] = data.date
            it[ReceiptDraftsTable.direction] = data.direction
            it[ReceiptDraftsTable.currency] = data.currency
            it[ReceiptDraftsTable.totalAmount] = data.totalAmount?.toDbDecimal()
            it[ReceiptDraftsTable.vatAmount] = data.vatAmount?.toDbDecimal()
            it[ReceiptDraftsTable.receiptNumber] = data.receiptNumber
            it[ReceiptDraftsTable.paymentMethod] = data.paymentMethod
            it[ReceiptDraftsTable.notes] = data.notes
        }
        true
    }

    private suspend fun saveBankStatementDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        data: BankStatementDraftData,
    ): Boolean = dbQuery {
        BankStatementDraftsTable.insert {
            it[BankStatementDraftsTable.tenantId] = tenantId.value.toJavaUuid()
            it[BankStatementDraftsTable.documentId] = documentId.value.toJavaUuid()
            it[BankStatementDraftsTable.direction] = data.direction
            it[BankStatementDraftsTable.accountIban] = data.accountIban?.value
            it[BankStatementDraftsTable.openingBalance] = data.openingBalance?.toDbDecimal()
            it[BankStatementDraftsTable.closingBalance] = data.closingBalance?.toDbDecimal()
            it[BankStatementDraftsTable.periodStart] = data.periodStart
            it[BankStatementDraftsTable.periodEnd] = data.periodEnd
            it[BankStatementDraftsTable.notes] = data.notes
        }
        true
    }

    // ==========================================================================
    // Classified draft writer (generic helper)
    // ==========================================================================

    /**
     * Generic helper to insert a classified draft into any table that follows
     * the standard classified schema (id, tenant_id, document_id, direction).
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun insertClassifiedDraft(
        tenantId: TenantId,
        documentId: DocumentId,
        table: UUIDTable,
        direction: DocumentDirection,
    ): Boolean = dbQuery {
        val tenantCol = table.columns.first { it.name == "tenant_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>
        val docCol = table.columns.first { it.name == "document_id" }
            as org.jetbrains.exposed.v1.core.Column<java.util.UUID>
        val dirCol = table.columns.first { it.name == "direction" }
            as org.jetbrains.exposed.v1.core.Column<DocumentDirection>

        table.insert {
            it[tenantCol] = tenantId.value.toJavaUuid()
            it[docCol] = documentId.value.toJavaUuid()
            it[dirCol] = direction
        }
        true
    }
}

// =============================================================================
// Entity -> DocDto conversions
// =============================================================================

private fun InvoiceDraftEntity.toDocDto(): DocDto.Invoice.Draft = DocDto.Invoice.Draft(
    direction = direction,
    invoiceNumber = invoiceNumber,
    issueDate = issueDate,
    dueDate = dueDate,
    currency = currency,
    subtotalAmount = subtotalAmount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    lineItems = items.map { it.toDocLineItem() },
    iban = senderIban,
    notes = notes,
    counterparty = tech.dokus.domain.model.PartyDraft(), // counterparty resolved separately via ResolvedContact
)

private fun InvoiceDraftItemEntity.toDocLineItem(): tech.dokus.domain.model.DocLineItem =
    tech.dokus.domain.model.DocLineItem(
        description = description,
        quantity = quantity?.let { tech.dokus.domain.Quantity(it) },
        unitPrice = unitPrice,
        vatRate = vatRate,
        netAmount = lineTotal,
        vatAmount = vatAmount,
        sortOrder = sortOrder,
    )

private fun CreditNoteDraftEntity.toDocDto(): DocDto.CreditNote.Draft = DocDto.CreditNote.Draft(
    direction = direction,
    creditNoteNumber = creditNoteNumber,
    issueDate = issueDate,
    currency = currency,
    subtotalAmount = subtotalAmount,
    vatAmount = vatAmount,
    totalAmount = totalAmount,
    lineItems = items.map { it.toDocLineItem() },
    reason = reason,
    notes = notes,
    originalInvoiceNumber = originalInvoiceNumber,
    counterparty = tech.dokus.domain.model.PartyDraft(),
)

private fun CreditNoteDraftItemEntity.toDocLineItem(): tech.dokus.domain.model.DocLineItem =
    tech.dokus.domain.model.DocLineItem(
        description = description,
        quantity = quantity?.let { tech.dokus.domain.Quantity(it) },
        unitPrice = unitPrice,
        vatRate = vatRate,
        netAmount = lineTotal,
        vatAmount = vatAmount,
        sortOrder = sortOrder,
    )

private fun ReceiptDraftEntity.toDocDto(): DocDto.Receipt.Draft = DocDto.Receipt.Draft(
    direction = direction,
    merchantName = merchantName,
    merchantVat = merchantVat,
    date = date,
    currency = currency,
    totalAmount = totalAmount,
    vatAmount = vatAmount,
    receiptNumber = receiptNumber,
    paymentMethod = paymentMethod,
    notes = notes,
)

private fun BankStatementDraftEntity.toDocDto(): DocDto.BankStatement.Draft = DocDto.BankStatement.Draft(
    direction = direction,
    accountIban = accountIban,
    openingBalance = openingBalance,
    closingBalance = closingBalance,
    periodStart = periodStart,
    periodEnd = periodEnd,
    notes = notes,
)

// =============================================================================
// Table dispatch helper
// =============================================================================

@Suppress("CyclomaticComplexMethod")
private fun draftTableFor(type: DocumentType): UUIDTable? = when (type) {
    DocumentType.Invoice -> InvoiceDraftsTable
    DocumentType.CreditNote -> CreditNoteDraftsTable
    DocumentType.Receipt -> ReceiptDraftsTable
    DocumentType.BankStatement -> BankStatementDraftsTable
    DocumentType.ProForma -> ProFormaDraftsTable
    DocumentType.Quote -> QuoteDraftsTable
    DocumentType.OrderConfirmation -> OrderConfirmationDraftsTable
    DocumentType.DeliveryNote -> DeliveryNoteDraftsTable
    DocumentType.Reminder -> ReminderDraftsTable
    DocumentType.StatementOfAccount -> StatementOfAccountDraftsTable
    DocumentType.PurchaseOrder -> PurchaseOrderDraftsTable
    DocumentType.ExpenseClaim -> ExpenseClaimDraftsTable
    DocumentType.BankFee -> BankFeeDraftsTable
    DocumentType.InterestStatement -> InterestStatementDraftsTable
    DocumentType.PaymentConfirmation -> PaymentConfirmationDraftsTable
    DocumentType.VatReturn -> VatReturnDraftsTable
    DocumentType.VatListing -> VatListingDraftsTable
    DocumentType.VatAssessment -> VatAssessmentDraftsTable
    DocumentType.IcListing -> IcListingDraftsTable
    DocumentType.OssReturn -> OssReturnDraftsTable
    DocumentType.CorporateTax -> CorporateTaxDraftsTable
    DocumentType.CorporateTaxAdvance -> CorporateTaxAdvanceDraftsTable
    DocumentType.TaxAssessment -> TaxAssessmentDraftsTable
    DocumentType.PersonalTax -> PersonalTaxDraftsTable
    DocumentType.WithholdingTax -> WithholdingTaxDraftsTable
    DocumentType.SocialContribution -> SocialContributionDraftsTable
    DocumentType.SocialFund -> SocialFundDraftsTable
    DocumentType.SelfEmployedContribution -> SelfEmployedContributionDraftsTable
    DocumentType.Vapz -> VapzDraftsTable
    DocumentType.SalarySlip -> SalarySlipDraftsTable
    DocumentType.PayrollSummary -> PayrollSummaryDraftsTable
    DocumentType.EmploymentContract -> EmploymentContractDraftsTable
    DocumentType.Dimona -> DimonaDraftsTable
    DocumentType.C4 -> C4DraftsTable
    DocumentType.HolidayPay -> HolidayPayDraftsTable
    DocumentType.Contract -> ContractDraftsTable
    DocumentType.Lease -> LeaseDraftsTable
    DocumentType.Loan -> LoanDraftsTable
    DocumentType.Insurance -> InsuranceDraftsTable
    DocumentType.Dividend -> DividendDraftsTable
    DocumentType.ShareholderRegister -> ShareholderRegisterDraftsTable
    DocumentType.CompanyExtract -> CompanyExtractDraftsTable
    DocumentType.AnnualAccounts -> AnnualAccountsDraftsTable
    DocumentType.BoardMinutes -> BoardMinutesDraftsTable
    DocumentType.Subsidy -> SubsidyDraftsTable
    DocumentType.Fine -> FineDraftsTable
    DocumentType.Permit -> PermitDraftsTable
    DocumentType.CustomsDeclaration -> CustomsDeclarationDraftsTable
    DocumentType.Intrastat -> IntrastatDraftsTable
    DocumentType.DepreciationSchedule -> DepreciationScheduleDraftsTable
    DocumentType.Inventory -> InventoryDraftsTable
    DocumentType.Other -> OtherDraftsTable
    DocumentType.Unknown -> null
}
