package tech.dokus.features.ai.coordinator

import tech.dokus.features.ai.agents.DocumentClassificationAgent
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.ensemble.ConflictReport
import tech.dokus.features.ai.ensemble.ConsensusEngine
import tech.dokus.features.ai.ensemble.ConsensusResult
import tech.dokus.features.ai.ensemble.PerceptionEnsemble
import tech.dokus.features.ai.judgment.JudgmentAgent
import tech.dokus.features.ai.judgment.JudgmentConfig
import tech.dokus.features.ai.judgment.JudgmentContext
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.retry.FeedbackDrivenRetryAgent
import tech.dokus.features.ai.retry.RetryResult
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.AuditStatus
import tech.dokus.features.ai.validation.ExtractionAuditService
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Orchestrates the 5-Layer Autonomous Processing Pipeline.
 *
 * ## Architecture
 *
 * ```
 * Document Upload
 *        │
 *        ▼
 * Layer 0: Classification ──────► REJECT if UNKNOWN
 *        │
 *        ▼
 * Layer 1: Perception Ensemble ──► Run fast + expert models in parallel
 *        │
 *        ▼
 * Layer 2: Consensus Engine ─────► Merge extractions, detect conflicts
 *        │
 *        ▼
 * Layer 3: Legally-Aware Auditor ► Math, checksums, VAT rates
 *        │
 *    ┌───┴───┐
 *    │Failed?│
 *    └───┬───┘
 *        │ Yes
 *        ▼
 * Layer 4: Self-Correction ──────► Retry with specific feedback
 *        │
 *        ▼
 * Layer 5: Judgment Agent ───────► AUTO_APPROVE / NEEDS_REVIEW / REJECT
 * ```
 *
 * ## Design Philosophy
 *
 * **"Silence"**: The goal is for 95%+ of documents to be processed silently
 * without user intervention. Users only see documents when the system
 * cannot prove correctness.
 *
 * ## Key Design Decisions
 *
 * 1. **Type-Specific Processing**: Each document type (INVOICE, BILL, etc.)
 *    has its own extraction, consensus, and audit paths. This ensures
 *    domain-specific validation rules are applied correctly.
 *
 * 2. **Graceful Degradation**: If the expert model fails, we fall back to
 *    the fast model. If both fail, we reject early.
 *
 * 3. **Provenance Tracking**: The result includes full details of each layer's
 *    output for debugging and audit purposes.
 *
 * 4. **Configurable Layers**: Each layer can be enabled/disabled via config.
 *    For example, `enableEnsemble = false` runs only the expert model.
 *
 * @see IntelligenceMode for configuration (single source of truth)
 * @see AutonomousResult for result types
 */
class AutonomousProcessingCoordinator(
    private val classificationAgent: DocumentClassificationAgent,
    private val mode: IntelligenceMode
) {
    private val logger = loggerFor()

    // Lazily initialized components - set via builder methods
    private var invoiceEnsemble: PerceptionEnsemble<ExtractedInvoiceData>? = null
    private var billEnsemble: PerceptionEnsemble<ExtractedBillData>? = null
    private var receiptEnsemble: PerceptionEnsemble<ExtractedReceiptData>? = null
    private var expenseEnsemble: PerceptionEnsemble<ExtractedExpenseData>? = null

    private var invoiceFastAgent: ExtractionAgent<ExtractedInvoiceData>? = null
    private var invoiceExpertAgent: ExtractionAgent<ExtractedInvoiceData>? = null
    private var billFastAgent: ExtractionAgent<ExtractedBillData>? = null
    private var billExpertAgent: ExtractionAgent<ExtractedBillData>? = null
    private var receiptFastAgent: ExtractionAgent<ExtractedReceiptData>? = null
    private var receiptExpertAgent: ExtractionAgent<ExtractedReceiptData>? = null
    private var expenseFastAgent: ExtractionAgent<ExtractedExpenseData>? = null
    private var expenseExpertAgent: ExtractionAgent<ExtractedExpenseData>? = null

    private var invoiceRetryAgent: FeedbackDrivenRetryAgent<ExtractedInvoiceData>? = null
    private var billRetryAgent: FeedbackDrivenRetryAgent<ExtractedBillData>? = null
    private var receiptRetryAgent: FeedbackDrivenRetryAgent<ExtractedReceiptData>? = null
    private var expenseRetryAgent: FeedbackDrivenRetryAgent<ExtractedExpenseData>? = null

    private val consensusEngine = ConsensusEngine()
    private val auditService = ExtractionAuditService()
    private var judgmentAgent: JudgmentAgent = JudgmentAgent.deterministic(JudgmentConfig.DEFAULT)

    // Configuration defaults (can be overridden via builder)
    private var minClassificationConfidence: Double = 0.3
    private var failFastOnUnknownType: Boolean = true
    private var useLlmForJudgment: Boolean = false

    // =========================================================================
    // Builder Methods for Setting Up Agents
    // =========================================================================

    /**
     * Configure invoice extraction agents.
     */
    fun withInvoiceAgents(
        fastAgent: ExtractionAgent<ExtractedInvoiceData>,
        expertAgent: ExtractionAgent<ExtractedInvoiceData>
    ): AutonomousProcessingCoordinator {
        invoiceFastAgent = fastAgent
        invoiceExpertAgent = expertAgent
        if (mode.enableEnsemble) {
            invoiceEnsemble = PerceptionEnsemble.of(fastAgent, expertAgent, mode.parallelExtraction)
        }
        return this
    }

    /**
     * Configure bill extraction agents.
     */
    fun withBillAgents(
        fastAgent: ExtractionAgent<ExtractedBillData>,
        expertAgent: ExtractionAgent<ExtractedBillData>
    ): AutonomousProcessingCoordinator {
        billFastAgent = fastAgent
        billExpertAgent = expertAgent
        if (mode.enableEnsemble) {
            billEnsemble = PerceptionEnsemble.of(fastAgent, expertAgent, mode.parallelExtraction)
        }
        return this
    }

    /**
     * Configure receipt extraction agents.
     */
    fun withReceiptAgents(
        fastAgent: ExtractionAgent<ExtractedReceiptData>,
        expertAgent: ExtractionAgent<ExtractedReceiptData>
    ): AutonomousProcessingCoordinator {
        receiptFastAgent = fastAgent
        receiptExpertAgent = expertAgent
        if (mode.enableEnsemble) {
            receiptEnsemble = PerceptionEnsemble.of(fastAgent, expertAgent, mode.parallelExtraction)
        }
        return this
    }

    /**
     * Configure expense extraction agents.
     */
    fun withExpenseAgents(
        fastAgent: ExtractionAgent<ExtractedExpenseData>,
        expertAgent: ExtractionAgent<ExtractedExpenseData>
    ): AutonomousProcessingCoordinator {
        expenseFastAgent = fastAgent
        expenseExpertAgent = expertAgent
        if (mode.enableEnsemble) {
            expenseEnsemble = PerceptionEnsemble.of(fastAgent, expertAgent, mode.parallelExtraction)
        }
        return this
    }

    /**
     * Configure retry agents for self-correction.
     */
    fun withRetryAgents(
        invoiceRetry: FeedbackDrivenRetryAgent<ExtractedInvoiceData>? = null,
        billRetry: FeedbackDrivenRetryAgent<ExtractedBillData>? = null,
        receiptRetry: FeedbackDrivenRetryAgent<ExtractedReceiptData>? = null,
        expenseRetry: FeedbackDrivenRetryAgent<ExtractedExpenseData>? = null
    ): AutonomousProcessingCoordinator {
        invoiceRetryAgent = invoiceRetry
        billRetryAgent = billRetry
        receiptRetryAgent = receiptRetry
        expenseRetryAgent = expenseRetry
        return this
    }

    /**
     * Configure judgment agent.
     */
    fun withJudgmentAgent(agent: JudgmentAgent): AutonomousProcessingCoordinator {
        judgmentAgent = agent
        return this
    }

    // =========================================================================
    // Main Processing Entry Point
    // =========================================================================

    /**
     * Process a document through the full autonomous pipeline.
     *
     * @param images Document page images
     * @param tenantContext Tenant context for classification
     * @return AutonomousResult with processing outcome
     */
    suspend fun process(
        images: List<DocumentImage>,
        tenantContext: AgentPrompt.TenantContext
    ): AutonomousResult {
        logger.info("Starting autonomous processing pipeline (${images.size} pages)")
        val startTime = System.currentTimeMillis()

        // =====================================================================
        // Layer 0: Classification
        // =====================================================================
        logger.info("Layer 0: Classifying document")
        val classification = classificationAgent.classify(images, tenantContext)
        logger.info(
            "Classification: ${classification.documentType} " +
                "(confidence: ${formatPercent(classification.confidence)})"
        )

        // Check classification confidence threshold
        if (classification.confidence < minClassificationConfidence) {
            logger.warn(
                "Classification confidence ${formatPercent(classification.confidence)} " +
                    "below threshold ${formatPercent(minClassificationConfidence)}"
            )
            return AutonomousResult.Rejected.lowConfidence(
                classification,
                minClassificationConfidence
            )
        }

        // Check for unknown document type
        if (classification.documentType == ClassifiedDocumentType.UNKNOWN) {
            if (failFastOnUnknownType) {
                logger.warn("Document type UNKNOWN, rejecting early")
                return AutonomousResult.Rejected.unknownDocumentType(classification)
            }
        }

        // =====================================================================
        // Route to type-specific processing
        // =====================================================================
        val result = when (classification.documentType) {
            ClassifiedDocumentType.INVOICE,
            ClassifiedDocumentType.CREDIT_NOTE,
            ClassifiedDocumentType.PRO_FORMA -> processInvoice(images, classification)

            ClassifiedDocumentType.BILL -> processBill(images, classification)
            ClassifiedDocumentType.RECEIPT -> processReceipt(images, classification)
            ClassifiedDocumentType.EXPENSE -> processExpense(images, classification)
            ClassifiedDocumentType.UNKNOWN -> processUnknown(classification)
        }

        val elapsed = System.currentTimeMillis() - startTime
        logger.info("Autonomous processing completed in ${elapsed}ms")

        return result
    }

    // =========================================================================
    // Type-Specific Processing Methods
    // =========================================================================

    /**
     * Process invoice (also handles CREDIT_NOTE and PRO_FORMA which share structure).
     */
    private suspend fun processInvoice(
        images: List<DocumentImage>,
        classification: DocumentClassification
    ): AutonomousResult {
        logger.info("Processing as INVOICE type")

        // Layer 1: Perception Ensemble
        val (extraction, conflictReport) = if (mode.enableEnsemble && invoiceEnsemble != null) {
            logger.info("Layer 1: Running perception ensemble")
            val ensembleResult = invoiceEnsemble!!.extract(images)

            if (!ensembleResult.hasAnyCandidate) {
                return AutonomousResult.Rejected.extractionFailed(
                    classification,
                    ensembleResult.fastError,
                    ensembleResult.expertError
                )
            }

            // Layer 2: Consensus Engine
            logger.info("Layer 2: Building consensus")
            val consensus = consensusEngine.mergeInvoices(
                ensembleResult.fastCandidate,
                ensembleResult.expertCandidate
            )
            extractConsensusData(consensus)
                ?: return AutonomousResult.Rejected.noDataExtracted(classification)
        } else {
            // Single model mode - use expert agent
            logger.info("Layer 1: Single model extraction (ensemble disabled)")
            val agent = invoiceExpertAgent ?: invoiceFastAgent
                ?: return AutonomousResult.Rejected(
                    reason = "No invoice extraction agent configured",
                    classification = classification,
                    stage = RejectionStage.EXTRACTION
                )

            try {
                agent.extract(images) to null
            } catch (e: Exception) {
                logger.error("Invoice extraction failed", e)
                return AutonomousResult.Rejected.extractionFailed(classification, null, e)
            }
        }

        // Layer 3: Legally-Aware Auditor
        logger.info("Layer 3: Running audit")
        val auditReport = auditService.auditInvoice(extraction)
        logger.info(
            "Audit result: ${auditReport.passedCount} passed, ${auditReport.failedCount} failed, " +
                "status: ${auditReport.overallStatus}"
        )

        // Layer 4: Self-Correction (if needed)
        val (finalExtraction, retryResult) = attemptSelfCorrection(
            extraction,
            auditReport,
            images,
            invoiceRetryAgent
        )

        // Re-audit if correction was attempted
        val finalAuditReport = if (retryResult is RetryResult.CorrectedOnRetry) {
            auditService.auditInvoice(finalExtraction)
        } else {
            auditReport
        }

        // Layer 5: Judgment
        return makeJudgment(
            extraction = finalExtraction,
            classification = classification,
            conflictReport = conflictReport,
            auditReport = finalAuditReport,
            retryResult = retryResult,
            essentialFieldsCheck = checkEssentialInvoiceFields(finalExtraction)
        )
    }

    /**
     * Process bill.
     */
    private suspend fun processBill(
        images: List<DocumentImage>,
        classification: DocumentClassification
    ): AutonomousResult {
        logger.info("Processing as BILL type")

        // Layer 1: Perception Ensemble
        val (extraction, conflictReport) = if (mode.enableEnsemble && billEnsemble != null) {
            logger.info("Layer 1: Running perception ensemble")
            val ensembleResult = billEnsemble!!.extract(images)

            if (!ensembleResult.hasAnyCandidate) {
                return AutonomousResult.Rejected.extractionFailed(
                    classification,
                    ensembleResult.fastError,
                    ensembleResult.expertError
                )
            }

            // Layer 2: Consensus Engine
            logger.info("Layer 2: Building consensus")
            val consensus = consensusEngine.mergeBills(
                ensembleResult.fastCandidate,
                ensembleResult.expertCandidate
            )
            extractConsensusData(consensus)
                ?: return AutonomousResult.Rejected.noDataExtracted(classification)
        } else {
            // Single model mode
            logger.info("Layer 1: Single model extraction (ensemble disabled)")
            val agent = billExpertAgent ?: billFastAgent
                ?: return AutonomousResult.Rejected(
                    reason = "No bill extraction agent configured",
                    classification = classification,
                    stage = RejectionStage.EXTRACTION
                )

            try {
                agent.extract(images) to null
            } catch (e: Exception) {
                logger.error("Bill extraction failed", e)
                return AutonomousResult.Rejected.extractionFailed(classification, null, e)
            }
        }

        // Layer 3: Audit
        logger.info("Layer 3: Running audit")
        val auditReport = auditService.auditBill(extraction)
        logger.info(
            "Audit result: ${auditReport.passedCount} passed, ${auditReport.failedCount} failed"
        )

        // Layer 4: Self-Correction
        val (finalExtraction, retryResult) = attemptSelfCorrection(
            extraction,
            auditReport,
            images,
            billRetryAgent
        )

        val finalAuditReport = if (retryResult is RetryResult.CorrectedOnRetry) {
            auditService.auditBill(finalExtraction)
        } else {
            auditReport
        }

        // Layer 5: Judgment
        return makeJudgment(
            extraction = finalExtraction,
            classification = classification,
            conflictReport = conflictReport,
            auditReport = finalAuditReport,
            retryResult = retryResult,
            essentialFieldsCheck = checkEssentialBillFields(finalExtraction)
        )
    }

    /**
     * Process receipt.
     */
    private suspend fun processReceipt(
        images: List<DocumentImage>,
        classification: DocumentClassification
    ): AutonomousResult {
        logger.info("Processing as RECEIPT type")

        // Layer 1: Perception Ensemble
        val (extraction, conflictReport) = if (mode.enableEnsemble && receiptEnsemble != null) {
            logger.info("Layer 1: Running perception ensemble")
            val ensembleResult = receiptEnsemble!!.extract(images)

            if (!ensembleResult.hasAnyCandidate) {
                return AutonomousResult.Rejected.extractionFailed(
                    classification,
                    ensembleResult.fastError,
                    ensembleResult.expertError
                )
            }

            // Layer 2: Consensus Engine
            logger.info("Layer 2: Building consensus")
            val consensus = consensusEngine.mergeReceipts(
                ensembleResult.fastCandidate,
                ensembleResult.expertCandidate
            )
            extractConsensusData(consensus)
                ?: return AutonomousResult.Rejected.noDataExtracted(classification)
        } else {
            // Single model mode
            logger.info("Layer 1: Single model extraction (ensemble disabled)")
            val agent = receiptExpertAgent ?: receiptFastAgent
                ?: return AutonomousResult.Rejected(
                    reason = "No receipt extraction agent configured",
                    classification = classification,
                    stage = RejectionStage.EXTRACTION
                )

            try {
                agent.extract(images) to null
            } catch (e: Exception) {
                logger.error("Receipt extraction failed", e)
                return AutonomousResult.Rejected.extractionFailed(classification, null, e)
            }
        }

        // Layer 3: Audit
        logger.info("Layer 3: Running audit")
        val auditReport = auditService.auditReceipt(extraction)
        logger.info(
            "Audit result: ${auditReport.passedCount} passed, ${auditReport.failedCount} failed"
        )

        // Layer 4: Self-Correction
        val (finalExtraction, retryResult) = attemptSelfCorrection(
            extraction,
            auditReport,
            images,
            receiptRetryAgent
        )

        val finalAuditReport = if (retryResult is RetryResult.CorrectedOnRetry) {
            auditService.auditReceipt(finalExtraction)
        } else {
            auditReport
        }

        // Layer 5: Judgment
        return makeJudgment(
            extraction = finalExtraction,
            classification = classification,
            conflictReport = conflictReport,
            auditReport = finalAuditReport,
            retryResult = retryResult,
            essentialFieldsCheck = checkEssentialReceiptFields(finalExtraction)
        )
    }

    /**
     * Process expense.
     */
    private suspend fun processExpense(
        images: List<DocumentImage>,
        classification: DocumentClassification
    ): AutonomousResult {
        logger.info("Processing as EXPENSE type")

        // Layer 1: Perception Ensemble
        val (extraction, conflictReport) = if (mode.enableEnsemble && expenseEnsemble != null) {
            logger.info("Layer 1: Running perception ensemble")
            val ensembleResult = expenseEnsemble!!.extract(images)

            if (!ensembleResult.hasAnyCandidate) {
                return AutonomousResult.Rejected.extractionFailed(
                    classification,
                    ensembleResult.fastError,
                    ensembleResult.expertError
                )
            }

            // Layer 2: Consensus Engine
            logger.info("Layer 2: Building consensus")
            val consensus = consensusEngine.mergeExpenses(
                ensembleResult.fastCandidate,
                ensembleResult.expertCandidate
            )
            extractConsensusData(consensus)
                ?: return AutonomousResult.Rejected.noDataExtracted(classification)
        } else {
            // Single model mode
            logger.info("Layer 1: Single model extraction (ensemble disabled)")
            val agent = expenseExpertAgent ?: expenseFastAgent
                ?: return AutonomousResult.Rejected(
                    reason = "No expense extraction agent configured",
                    classification = classification,
                    stage = RejectionStage.EXTRACTION
                )

            try {
                agent.extract(images) to null
            } catch (e: Exception) {
                logger.error("Expense extraction failed", e)
                return AutonomousResult.Rejected.extractionFailed(classification, null, e)
            }
        }

        // Layer 3: Audit
        logger.info("Layer 3: Running audit")
        val auditReport = auditService.auditExpense(extraction)
        logger.info(
            "Audit result: ${auditReport.passedCount} passed, ${auditReport.failedCount} failed"
        )

        // Layer 4: Self-Correction
        val (finalExtraction, retryResult) = attemptSelfCorrection(
            extraction,
            auditReport,
            images,
            expenseRetryAgent
        )

        val finalAuditReport = if (retryResult is RetryResult.CorrectedOnRetry) {
            auditService.auditExpense(finalExtraction)
        } else {
            auditReport
        }

        // Layer 5: Judgment
        return makeJudgment(
            extraction = finalExtraction,
            classification = classification,
            conflictReport = conflictReport,
            auditReport = finalAuditReport,
            retryResult = retryResult,
            essentialFieldsCheck = checkEssentialExpenseFields(finalExtraction)
        )
    }

    /**
     * Handle unknown document type (fallback processing).
     */
    private fun processUnknown(
        classification: DocumentClassification
    ): AutonomousResult {
        logger.warn("Processing UNKNOWN document type - limited processing available")

        // For unknown types, we can't do type-specific extraction
        // Just return a rejection with the classification info
        return AutonomousResult.Rejected(
            reason = "Document type could not be determined. Manual classification required.",
            classification = classification,
            stage = RejectionStage.CLASSIFICATION
        )
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Extract data from consensus result, handling all cases.
     */
    private fun <T> extractConsensusData(
        consensus: ConsensusResult<T>
    ): Pair<T, ConflictReport?>? {
        return when (consensus) {
            is ConsensusResult.NoData -> null
            is ConsensusResult.SingleSource -> consensus.data to null
            is ConsensusResult.Unanimous -> consensus.data to null
            is ConsensusResult.WithConflicts -> consensus.data to consensus.report
        }
    }

    /**
     * Attempt self-correction if audit failed and retry is enabled.
     */
    private suspend fun <T : Any> attemptSelfCorrection(
        extraction: T,
        auditReport: AuditReport,
        images: List<DocumentImage>,
        retryAgent: FeedbackDrivenRetryAgent<T>?
    ): Pair<T, RetryResult<T>?> {
        // Check if self-correction is needed and enabled
        if (!mode.enableSelfCorrection) {
            logger.debug("Self-correction disabled")
            return extraction to null
        }

        if (auditReport.overallStatus == AuditStatus.PASSED) {
            logger.debug("Audit passed, no self-correction needed")
            return extraction to RetryResult.NoRetryNeeded
        }

        if (auditReport.criticalFailures.isEmpty()) {
            logger.debug("No critical failures, skipping self-correction")
            return extraction to RetryResult.NoRetryNeeded
        }

        if (retryAgent == null) {
            logger.warn("Self-correction requested but no retry agent configured")
            return extraction to null
        }

        // Layer 4: Attempt self-correction
        logger.info(
            "Layer 4: Attempting self-correction for " +
                "${auditReport.criticalFailures.size} critical failures"
        )

        val retryResult = retryAgent.attemptCorrection(
            images = images,
            initialExtraction = extraction,
            initialAuditReport = auditReport
        )

        return when (retryResult) {
            is RetryResult.NoRetryNeeded -> extraction to retryResult
            is RetryResult.CorrectedOnRetry -> {
                logger.info(
                    "Self-correction succeeded on attempt ${retryResult.attempt}. " +
                        "Corrected fields: ${retryResult.correctedFields}"
                )
                retryResult.data to retryResult
            }
            is RetryResult.StillFailing -> {
                logger.warn(
                    "Self-correction failed after ${retryResult.attempts} attempts. " +
                        "Remaining failures: ${retryResult.remainingFailures.size}"
                )
                retryResult.data to retryResult
            }
        }
    }

    /**
     * Make final judgment decision.
     */
    private suspend fun <T : Any> makeJudgment(
        extraction: T,
        classification: DocumentClassification,
        conflictReport: ConflictReport?,
        auditReport: AuditReport,
        retryResult: RetryResult<T>?,
        essentialFieldsCheck: EssentialFieldsCheck
    ): AutonomousResult {
        logger.info("Layer 5: Making final judgment")

        val context = JudgmentContext(
            extractionConfidence = getExtractionConfidence(extraction),
            consensusReport = conflictReport,
            auditReport = auditReport,
            retryResult = retryResult,
            documentType = classification.documentType.name,
            hasEssentialFields = essentialFieldsCheck.hasAllFields,
            missingEssentialFields = essentialFieldsCheck.missingFields
        )

        val decision = judgmentAgent.judge(context, useLlm = useLlmForJudgment)
        logger.info(
            "Judgment: ${decision.outcome} (confidence: ${formatPercent(decision.confidence)})"
        )

        return AutonomousResult.Success(
            classification = classification,
            extraction = extraction,
            conflictReport = conflictReport,
            auditReport = auditReport,
            retryResult = retryResult,
            judgment = decision
        )
    }

    // =========================================================================
    // Essential Fields Checks
    // =========================================================================

    /**
     * Result of checking essential fields.
     */
    private data class EssentialFieldsCheck(
        val hasAllFields: Boolean,
        val missingFields: List<String>
    )

    private fun checkEssentialInvoiceFields(invoice: ExtractedInvoiceData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()
        if (invoice.totalAmount.isNullOrBlank()) missing.add("totalAmount")
        if (invoice.vendorName.isNullOrBlank()) missing.add("vendorName")
        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    private fun checkEssentialBillFields(bill: ExtractedBillData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()
        if (bill.totalAmount.isNullOrBlank()) missing.add("totalAmount")
        if (bill.supplierName.isNullOrBlank()) missing.add("supplierName")
        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    private fun checkEssentialReceiptFields(receipt: ExtractedReceiptData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()
        if (receipt.totalAmount.isNullOrBlank()) missing.add("totalAmount")
        if (receipt.merchantName.isNullOrBlank()) missing.add("merchantName")
        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    private fun checkEssentialExpenseFields(expense: ExtractedExpenseData): EssentialFieldsCheck {
        val missing = mutableListOf<String>()
        if (expense.totalAmount.isNullOrBlank()) missing.add("totalAmount")
        return EssentialFieldsCheck(missing.isEmpty(), missing)
    }

    /**
     * Get extraction confidence from any extraction type.
     */
    private fun getExtractionConfidence(extraction: Any): Double {
        return when (extraction) {
            is ExtractedInvoiceData -> extraction.confidence
            is ExtractedBillData -> extraction.confidence
            is ExtractedReceiptData -> extraction.confidence
            is ExtractedExpenseData -> extraction.confidence
            else -> 0.5 // Default confidence for unknown types
        }
    }

    private fun formatPercent(value: Double): String = "${(value * 100).toInt()}%"

    companion object {
        /**
         * Create a coordinator with the specified intelligence mode.
         *
         * @param classificationAgent The classification agent
         * @param mode The intelligence mode (single source of truth for processing strategy)
         */
        fun create(
            classificationAgent: DocumentClassificationAgent,
            mode: IntelligenceMode
        ): AutonomousProcessingCoordinator {
            return AutonomousProcessingCoordinator(classificationAgent, mode)
        }
    }
}
