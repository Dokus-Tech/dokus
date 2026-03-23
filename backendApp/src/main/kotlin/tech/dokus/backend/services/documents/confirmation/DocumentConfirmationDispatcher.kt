package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.services.banking.BankStatementProcessingService
import tech.dokus.backend.services.cashflow.matching.MatchingEngine
import tech.dokus.backend.services.documents.DocumentPurposeSimilarityService
import tech.dokus.backend.services.documents.RAGIndexingService
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
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
import tech.dokus.domain.model.toDocumentType
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Dispatches document confirmation to the appropriate type-specific service.
 * Eliminates duplicate `when(draftData)` blocks in routes and workers.
 */
class DocumentConfirmationDispatcher(
    private val invoiceService: InvoiceConfirmationService,
    private val receiptService: ReceiptConfirmationService,
    private val creditNoteService: CreditNoteConfirmationService,
    private val bankStatementProcessingService: BankStatementProcessingService,
    private val matchingEngine: MatchingEngine,
    private val documentRepository: DocumentRepository,
    private val purposeSimilarityService: DocumentPurposeSimilarityService,
    private val ragIndexingService: RAGIndexingService,
) {
    private val logger = loggerFor()

    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: DocumentDraftData,
        contactId: ContactId?
    ): Result<ConfirmationResult> {
        val confirmation = when (draftData) {
            is InvoiceDraftData -> invoiceService.confirm(tenantId, documentId, draftData, contactId)
            is ReceiptDraftData -> receiptService.confirm(tenantId, documentId, draftData, contactId)
            is CreditNoteDraftData -> creditNoteService.confirm(tenantId, documentId, draftData, contactId)
            is BankStatementDraftData -> confirmBankStatement(tenantId, documentId, draftData)
            is ProFormaDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is QuoteDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is OrderConfirmationDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is DeliveryNoteDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is ReminderDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is StatementOfAccountDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is PurchaseOrderDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is ExpenseClaimDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is BankFeeDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is InterestStatementDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is PaymentConfirmationDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is VatReturnDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is VatListingDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is VatAssessmentDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is IcListingDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is OssReturnDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is CorporateTaxDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is CorporateTaxAdvanceDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is TaxAssessmentDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is PersonalTaxDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is WithholdingTaxDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is SocialContributionDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is SocialFundDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is SelfEmployedContributionDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is VapzDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is SalarySlipDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is PayrollSummaryDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is EmploymentContractDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is DimonaDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is C4DraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is HolidayPayDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is ContractDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is LeaseDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is LoanDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is InsuranceDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is DividendDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is ShareholderRegisterDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is CompanyExtractDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is AnnualAccountsDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is BoardMinutesDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is SubsidyDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is FineDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is PermitDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is CustomsDeclarationDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is IntrastatDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is DepreciationScheduleDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is InventoryDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
            is OtherDraftData -> Result.failure(
                DokusException.BadRequest("Document type ${draftData.toDocumentType()} does not support confirmation")
            )
        }

        confirmation.onSuccess {
            runSuspendCatching {
                purposeSimilarityService.indexConfirmedDocument(
                    tenantId = tenantId,
                    documentId = documentId
                )
            }.onFailure { error ->
                logger.warn(
                    "Purpose similarity indexing failed after confirmation for document {}: {}",
                    documentId,
                    error.message
                )
            }

            // Index document for RAG-powered chat (best-effort)
            runSuspendCatching {
                ragIndexingService.indexDocument(tenantId, documentId)
            }.onFailure { error ->
                logger.warn(
                    "RAG indexing failed after confirmation for document {}: {}",
                    documentId,
                    error.message
                )
            }
        }

        return confirmation
    }

    private suspend fun confirmBankStatement(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: BankStatementDraftData,
    ): Result<ConfirmationResult> = runSuspendCatching {
        val nonExcludedRows = draftData.transactions.filter { !it.excluded }
        if (nonExcludedRows.isEmpty()) {
            throw DokusException.BadRequest("No transactions to confirm — all rows excluded")
        }

        // Build a minimal prepare result for persistence (account already resolved during initial processing)
        val prepareResult = bankStatementProcessingService.prepare(
            tenantId = tenantId,
            documentId = documentId,
            sourceId = null,
            draftData = draftData.copy(transactions = nonExcludedRows),
        )

        bankStatementProcessingService.persistTransactions(
            tenantId = tenantId,
            documentId = documentId,
            prepareResult = prepareResult,
        )

        documentRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        // Run matching on the newly persisted transactions
        runSuspendCatching {
            matchingEngine.matchBankStatement(tenantId, documentId)
        }.onFailure {
            logger.warn("Matching failed after bank statement confirmation {}: {}", documentId, it.message)
        }

        logger.info("Bank statement confirmed: {}", documentId)
        ConfirmationResult(
            entity = Unit,
            cashflowEntryId = null,
            documentId = documentId,
        )
    }
}
