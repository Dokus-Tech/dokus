package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.DocumentPurposeTemplateRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.DraftSummaryEntity
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.model.toDraftData
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
import tech.dokus.domain.model.resolvedCounterpartyName
import tech.dokus.domain.model.resolvedCounterpartyVat
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.isLinked
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.models.PurposeEnrichmentInput
import tech.dokus.foundation.backend.utils.loggerFor

private const val PurposeBaseMaxLength = 72

@Suppress("LongParameterList")
class DocumentPurposeService(
    private val documentRepository: DocumentRepository,
    private val templateRepository: DocumentPurposeTemplateRepository,
    private val similarityService: DocumentPurposeSimilarityService,
    private val processingAgent: DocumentProcessingAgent,
    private val draftRepository: DraftRepository,
) {
    private val logger = loggerFor()

    suspend fun enrichAfterContactResolution(
        tenantId: TenantId,
        documentId: DocumentId,
        documentType: DocumentType,
        draftData: DocumentDraftData,
        linkedContactId: ContactId?,
        currentDraft: DraftSummaryEntity
    ) {
        val supplierName = resolveSupplierName(draftData)
        val supplierDisplayName = normalizeSupplierDisplayName(supplierName)
        val counterpartyKey = currentDraft.counterpartyKey ?: computeCounterpartyKey(linkedContactId, draftData)
        val merchantToken = currentDraft.merchantToken ?: computeMerchantToken(supplierDisplayName)

        documentRepository.updatePurposeContext(
            documentId = documentId,
            tenantId = tenantId,
            counterpartyKey = counterpartyKey,
            merchantToken = merchantToken
        )

        if (currentDraft.purposeLocked && !currentDraft.purposeBase.isNullOrBlank()) {
            logger.debug("Skipping purpose enrichment for locked draft {}", documentId)
            return
        }

        val template = counterpartyKey?.let {
            templateRepository.findByCounterparty(
                tenantId = tenantId,
                counterpartyKey = it,
                documentType = documentType
            )
        }

        val servicePeriodStart = PurposePeriodHeuristics.detectServicePeriodStart(draftData)
        val ragCandidates = if (template == null) {
            val similarityCandidates = similarityService.findCandidates(
                tenantId = tenantId,
                documentType = documentType,
                counterpartyKey = counterpartyKey,
                merchantToken = merchantToken,
                queryPurposeBase = deriveSimilarityQueryPurposeBase(draftData, documentType),
                minSimilarity = 0.78f,
                topK = 3
            )
            if (similarityCandidates.isNotEmpty()) {
                similarityCandidates
            } else {
                when {
                    !counterpartyKey.isNullOrBlank() -> {
                        documentRepository.listConfirmedPurposeBasesByCounterparty(
                            tenantId = tenantId,
                            counterpartyKey = counterpartyKey,
                            documentType = documentType,
                            limit = 5
                        )
                    }

                    !merchantToken.isNullOrBlank() -> {
                        documentRepository.listConfirmedPurposeBasesByMerchantToken(
                            tenantId = tenantId,
                            merchantToken = merchantToken,
                            documentType = documentType,
                            limit = 5
                        )
                    }

                    else -> emptyList()
                }
            }
        } else {
            emptyList()
        }

        val enrichment = processingAgent.enrichPurpose(
            PurposeEnrichmentInput(
                tenantId = tenantId,
                documentType = documentType,
                supplierDisplayName = supplierDisplayName,
                issueDate = extractIssueDate(draftData),
                servicePeriodStart = servicePeriodStart,
                templatePurposeBase = template?.purposeBase,
                templatePeriodMode = template?.periodMode,
                ragPurposeBaseCandidates = ragCandidates,
                fallbackPurposeBase = deriveFallbackPurposeBase(draftData)
            )
        )

        val purposeBase = enrichment.purposeBase?.trim()?.take(PurposeBaseMaxLength)
        val purposeRendered = enrichment.purposeRendered?.trim()?.take(80)
        if (purposeBase.isNullOrBlank() || purposeRendered.isNullOrBlank()) {
            return
        }

        documentRepository.updatePurposeFields(
            documentId = documentId,
            tenantId = tenantId,
            purposeBase = purposeBase,
            purposePeriodYear = enrichment.purposePeriodYear,
            purposePeriodMonth = enrichment.purposePeriodMonth,
            purposeRendered = purposeRendered,
            purposeSource = enrichment.purposeSource,
            purposeLocked = false,
            purposePeriodMode = enrichment.purposePeriodMode
        )

        if (template != null && !counterpartyKey.isNullOrBlank()) {
            templateRepository.upsert(
                tenantId = tenantId,
                counterpartyKey = counterpartyKey,
                documentType = documentType,
                purposeBase = template.purposeBase,
                periodMode = template.periodMode,
                confidence = template.confidence,
                incrementUsage = true
            )
        }
    }

    suspend fun applyUserPurposeEdit(
        tenantId: TenantId,
        documentId: DocumentId,
        draft: DraftSummaryEntity,
        purpose: String,
        purposePeriodMode: PurposePeriodMode?
    ) {
        val docDto = draftRepository.getDraftAsDocDto(tenantId, documentId, draft.documentType)
        val draftData = docDto?.toDraftData() ?: return
        val base = purpose.trim().replace(Regex("\\s+"), " ").take(PurposeBaseMaxLength)
        if (base.isBlank()) return

        val mode = purposePeriodMode ?: draft.purposePeriodMode
        val periodDate = extractPeriodDate(draftData, mode)
        val rendered = renderPurpose(
            supplierDisplayName = normalizeSupplierDisplayName(resolveSupplierName(draftData)),
            purposeBase = base,
            purposePeriodYear = periodDate?.year,
            purposePeriodMonth = periodDate?.monthNumber,
            periodMode = mode
        )?.take(80)

        documentRepository.updatePurposeFields(
            documentId = documentId,
            tenantId = tenantId,
            purposeBase = base,
            purposePeriodYear = periodDate?.year,
            purposePeriodMonth = periodDate?.monthNumber,
            purposeRendered = rendered,
            purposeSource = DocumentPurposeSource.User,
            purposeLocked = true,
            purposePeriodMode = mode
        )

        val counterparty = draft.counterparty
        val draftContactId = if (counterparty.isLinked()) counterparty.contactId else null
        val counterpartyKey = draft.counterpartyKey ?: computeCounterpartyKey(draftContactId, draftData)
        if (!counterpartyKey.isNullOrBlank()) {
            templateRepository.upsert(
                tenantId = tenantId,
                counterpartyKey = counterpartyKey,
                documentType = draft.documentType ?: draftData.toDocumentType(),
                purposeBase = base,
                periodMode = mode,
                confidence = 1.0,
                incrementUsage = true
            )
        }

        if (draft.documentStatus == DocumentStatus.Confirmed) {
            similarityService.indexConfirmedDocument(tenantId, documentId)
        }
    }

    private fun computeCounterpartyKey(linkedContactId: ContactId?, draftData: DocumentDraftData): String? {
        if (linkedContactId != null) return "contact:$linkedContactId"
        val vat = extractCounterpartyVat(draftData)?.let { VatNumber.normalize(it.value) }
        return vat?.takeIf { it.isNotBlank() }?.let { "vat:$it" }
    }

    private fun computeMerchantToken(supplierDisplayName: String?): String? {
        val normalized = supplierDisplayName
            ?.lowercase()
            ?.replace(Regex("[^a-z0-9 ]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?: return null
        return normalized.split(" ").firstOrNull { it.length >= 3 } ?: normalized.takeIf { it.isNotBlank() }
    }

    private fun resolveSupplierName(draftData: DocumentDraftData): String? = when (draftData) {
        is InvoiceDraftData -> when (draftData.direction) {
            DocumentDirection.Inbound -> draftData.seller.name
            DocumentDirection.Outbound -> draftData.buyer.name
            DocumentDirection.Neutral -> draftData.seller.name ?: draftData.buyer.name
            DocumentDirection.Unknown -> draftData.seller.name ?: draftData.buyer.name
        }

        is CreditNoteDraftData -> draftData.resolvedCounterpartyName

        is ReceiptDraftData -> draftData.merchantName
        is BankStatementDraftData -> draftData.transactions.firstNotNullOfOrNull { it.counterparty.name }
        is ProFormaDraftData,
        is QuoteDraftData,
        is OrderConfirmationDraftData,
        is DeliveryNoteDraftData,
        is ReminderDraftData,
        is StatementOfAccountDraftData,
        is PurchaseOrderDraftData,
        is ExpenseClaimDraftData,
        is BankFeeDraftData,
        is InterestStatementDraftData,
        is PaymentConfirmationDraftData,
        is VatReturnDraftData,
        is VatListingDraftData,
        is VatAssessmentDraftData,
        is IcListingDraftData,
        is OssReturnDraftData,
        is CorporateTaxDraftData,
        is CorporateTaxAdvanceDraftData,
        is TaxAssessmentDraftData,
        is PersonalTaxDraftData,
        is WithholdingTaxDraftData,
        is SocialContributionDraftData,
        is SocialFundDraftData,
        is SelfEmployedContributionDraftData,
        is VapzDraftData,
        is SalarySlipDraftData,
        is PayrollSummaryDraftData,
        is EmploymentContractDraftData,
        is DimonaDraftData,
        is C4DraftData,
        is HolidayPayDraftData,
        is ContractDraftData,
        is LeaseDraftData,
        is LoanDraftData,
        is InsuranceDraftData,
        is DividendDraftData,
        is ShareholderRegisterDraftData,
        is CompanyExtractDraftData,
        is AnnualAccountsDraftData,
        is BoardMinutesDraftData,
        is SubsidyDraftData,
        is FineDraftData,
        is PermitDraftData,
        is CustomsDeclarationDraftData,
        is IntrastatDraftData,
        is DepreciationScheduleDraftData,
        is InventoryDraftData,
        is OtherDraftData -> null
    }

    private fun extractCounterpartyVat(draftData: DocumentDraftData): VatNumber? = when (draftData) {
        is InvoiceDraftData -> when (draftData.direction) {
            DocumentDirection.Inbound -> draftData.seller.vat
            DocumentDirection.Outbound -> draftData.buyer.vat
            DocumentDirection.Neutral -> draftData.seller.vat ?: draftData.buyer.vat
            DocumentDirection.Unknown -> draftData.seller.vat ?: draftData.buyer.vat
        }

        is CreditNoteDraftData -> draftData.resolvedCounterpartyVat

        is ReceiptDraftData -> draftData.merchantVat
        is BankStatementDraftData -> null
        is ProFormaDraftData,
        is QuoteDraftData,
        is OrderConfirmationDraftData,
        is DeliveryNoteDraftData,
        is ReminderDraftData,
        is StatementOfAccountDraftData,
        is PurchaseOrderDraftData,
        is ExpenseClaimDraftData,
        is BankFeeDraftData,
        is InterestStatementDraftData,
        is PaymentConfirmationDraftData,
        is VatReturnDraftData,
        is VatListingDraftData,
        is VatAssessmentDraftData,
        is IcListingDraftData,
        is OssReturnDraftData,
        is CorporateTaxDraftData,
        is CorporateTaxAdvanceDraftData,
        is TaxAssessmentDraftData,
        is PersonalTaxDraftData,
        is WithholdingTaxDraftData,
        is SocialContributionDraftData,
        is SocialFundDraftData,
        is SelfEmployedContributionDraftData,
        is VapzDraftData,
        is SalarySlipDraftData,
        is PayrollSummaryDraftData,
        is EmploymentContractDraftData,
        is DimonaDraftData,
        is C4DraftData,
        is HolidayPayDraftData,
        is ContractDraftData,
        is LeaseDraftData,
        is LoanDraftData,
        is InsuranceDraftData,
        is DividendDraftData,
        is ShareholderRegisterDraftData,
        is CompanyExtractDraftData,
        is AnnualAccountsDraftData,
        is BoardMinutesDraftData,
        is SubsidyDraftData,
        is FineDraftData,
        is PermitDraftData,
        is CustomsDeclarationDraftData,
        is IntrastatDraftData,
        is DepreciationScheduleDraftData,
        is InventoryDraftData,
        is OtherDraftData -> null
    }

    private fun extractIssueDate(draftData: DocumentDraftData): LocalDate? = when (draftData) {
        is InvoiceDraftData -> draftData.issueDate
        is CreditNoteDraftData -> draftData.issueDate
        is ReceiptDraftData -> draftData.date
        is BankStatementDraftData -> draftData.transactions.firstNotNullOfOrNull { it.transactionDate }
        is ProFormaDraftData,
        is QuoteDraftData,
        is OrderConfirmationDraftData,
        is DeliveryNoteDraftData,
        is ReminderDraftData,
        is StatementOfAccountDraftData,
        is PurchaseOrderDraftData,
        is ExpenseClaimDraftData,
        is BankFeeDraftData,
        is InterestStatementDraftData,
        is PaymentConfirmationDraftData,
        is VatReturnDraftData,
        is VatListingDraftData,
        is VatAssessmentDraftData,
        is IcListingDraftData,
        is OssReturnDraftData,
        is CorporateTaxDraftData,
        is CorporateTaxAdvanceDraftData,
        is TaxAssessmentDraftData,
        is PersonalTaxDraftData,
        is WithholdingTaxDraftData,
        is SocialContributionDraftData,
        is SocialFundDraftData,
        is SelfEmployedContributionDraftData,
        is VapzDraftData,
        is SalarySlipDraftData,
        is PayrollSummaryDraftData,
        is EmploymentContractDraftData,
        is DimonaDraftData,
        is C4DraftData,
        is HolidayPayDraftData,
        is ContractDraftData,
        is LeaseDraftData,
        is LoanDraftData,
        is InsuranceDraftData,
        is DividendDraftData,
        is ShareholderRegisterDraftData,
        is CompanyExtractDraftData,
        is AnnualAccountsDraftData,
        is BoardMinutesDraftData,
        is SubsidyDraftData,
        is FineDraftData,
        is PermitDraftData,
        is CustomsDeclarationDraftData,
        is IntrastatDraftData,
        is DepreciationScheduleDraftData,
        is InventoryDraftData,
        is OtherDraftData -> null
    }

    private fun extractPeriodDate(draftData: DocumentDraftData, mode: PurposePeriodMode): LocalDate? {
        return when (mode) {
            PurposePeriodMode.IssueMonth -> extractIssueDate(draftData)
            PurposePeriodMode.ServicePeriod -> PurposePeriodHeuristics.detectServicePeriodStart(draftData) ?: extractIssueDate(draftData)
            PurposePeriodMode.None -> null
        }
    }

    private fun normalizeSupplierDisplayName(name: String?): String? {
        val cleaned = name
            ?.trim()
            ?.replace(Regex("\\s+"), " ")
            ?.replace(Regex("[,]"), "")
            ?: return null
        if (cleaned.isBlank()) return null

        val suffixes = setOf(
            "llc", "l.l.c", "ltd", "limited", "inc", "corp", "corporation",
            "bv", "b.v", "bvba", "nv", "n.v", "gmbh", "srl", "sa"
        )
        val tokens = cleaned.split(" ").toMutableList()
        while (tokens.isNotEmpty() && suffixes.contains(tokens.last().lowercase().trim('.'))) {
            tokens.removeLast()
        }
        return if (tokens.isEmpty()) cleaned else tokens.joinToString(" ")
    }

    private fun deriveFallbackPurposeBase(draftData: DocumentDraftData): String? {
        val lineHint = when (draftData) {
            is InvoiceDraftData -> draftData.lineItems.firstOrNull()?.description
            is CreditNoteDraftData -> draftData.lineItems.firstOrNull()?.description
            is ReceiptDraftData -> draftData.lineItems.firstOrNull()?.description
            is BankStatementDraftData -> draftData.transactions.firstOrNull()?.descriptionRaw
            is ProFormaDraftData,
            is QuoteDraftData,
            is OrderConfirmationDraftData,
            is DeliveryNoteDraftData,
            is ReminderDraftData,
            is StatementOfAccountDraftData,
            is PurchaseOrderDraftData,
            is ExpenseClaimDraftData,
            is BankFeeDraftData,
            is InterestStatementDraftData,
            is PaymentConfirmationDraftData,
            is VatReturnDraftData,
            is VatListingDraftData,
            is VatAssessmentDraftData,
            is IcListingDraftData,
            is OssReturnDraftData,
            is CorporateTaxDraftData,
            is CorporateTaxAdvanceDraftData,
            is TaxAssessmentDraftData,
            is PersonalTaxDraftData,
            is WithholdingTaxDraftData,
            is SocialContributionDraftData,
            is SocialFundDraftData,
            is SelfEmployedContributionDraftData,
            is VapzDraftData,
            is SalarySlipDraftData,
            is PayrollSummaryDraftData,
            is EmploymentContractDraftData,
            is DimonaDraftData,
            is C4DraftData,
            is HolidayPayDraftData,
            is ContractDraftData,
            is LeaseDraftData,
            is LoanDraftData,
            is InsuranceDraftData,
            is DividendDraftData,
            is ShareholderRegisterDraftData,
            is CompanyExtractDraftData,
            is AnnualAccountsDraftData,
            is BoardMinutesDraftData,
            is SubsidyDraftData,
            is FineDraftData,
            is PermitDraftData,
            is CustomsDeclarationDraftData,
            is IntrastatDraftData,
            is DepreciationScheduleDraftData,
            is InventoryDraftData,
            is OtherDraftData -> null
        }?.trim()?.take(PurposeBaseMaxLength)
        return lineHint?.takeIf { it.isNotBlank() }
    }

    private fun deriveSimilarityQueryPurposeBase(
        draftData: DocumentDraftData,
        documentType: DocumentType
    ): String {
        val explicitHint = when (draftData) {
            is InvoiceDraftData -> draftData.notes
            is CreditNoteDraftData -> draftData.reason ?: draftData.notes
            is ReceiptDraftData -> draftData.notes
            is BankStatementDraftData -> draftData.notes
            is ProFormaDraftData,
            is QuoteDraftData,
            is OrderConfirmationDraftData,
            is DeliveryNoteDraftData,
            is ReminderDraftData,
            is StatementOfAccountDraftData,
            is PurchaseOrderDraftData,
            is ExpenseClaimDraftData,
            is BankFeeDraftData,
            is InterestStatementDraftData,
            is PaymentConfirmationDraftData,
            is VatReturnDraftData,
            is VatListingDraftData,
            is VatAssessmentDraftData,
            is IcListingDraftData,
            is OssReturnDraftData,
            is CorporateTaxDraftData,
            is CorporateTaxAdvanceDraftData,
            is TaxAssessmentDraftData,
            is PersonalTaxDraftData,
            is WithholdingTaxDraftData,
            is SocialContributionDraftData,
            is SocialFundDraftData,
            is SelfEmployedContributionDraftData,
            is VapzDraftData,
            is SalarySlipDraftData,
            is PayrollSummaryDraftData,
            is EmploymentContractDraftData,
            is DimonaDraftData,
            is C4DraftData,
            is HolidayPayDraftData,
            is ContractDraftData,
            is LeaseDraftData,
            is LoanDraftData,
            is InsuranceDraftData,
            is DividendDraftData,
            is ShareholderRegisterDraftData,
            is CompanyExtractDraftData,
            is AnnualAccountsDraftData,
            is BoardMinutesDraftData,
            is SubsidyDraftData,
            is FineDraftData,
            is PermitDraftData,
            is CustomsDeclarationDraftData,
            is IntrastatDraftData,
            is DepreciationScheduleDraftData,
            is InventoryDraftData,
            is OtherDraftData -> null
        }?.trim()?.take(PurposeBaseMaxLength)
        return deriveFallbackPurposeBase(draftData)
            ?: explicitHint?.takeIf { it.isNotBlank() }
            ?: when (documentType) {
                DocumentType.Invoice -> "invoice"
                DocumentType.CreditNote -> "credit note"
                DocumentType.Receipt -> "receipt"
                else -> "document"
            }
    }

    private fun renderPurpose(
        supplierDisplayName: String?,
        purposeBase: String?,
        purposePeriodYear: Int?,
        purposePeriodMonth: Int?,
        periodMode: PurposePeriodMode
    ): String? {
        val base = purposeBase?.takeIf { it.isNotBlank() } ?: return null
        val periodLabel = if (periodMode == PurposePeriodMode.None || purposePeriodYear == null || purposePeriodMonth == null) {
            null
        } else {
            "${monthLabel(purposePeriodMonth)} $purposePeriodYear"
        }
        return buildString {
            if (!supplierDisplayName.isNullOrBlank()) {
                append(supplierDisplayName)
                append(" - ")
            }
            append(base)
            if (!periodLabel.isNullOrBlank()) {
                append(" ")
                append(periodLabel)
            }
        }
    }

    private fun monthLabel(monthNumber: Int): String = when (monthNumber) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Month"
    }

}
