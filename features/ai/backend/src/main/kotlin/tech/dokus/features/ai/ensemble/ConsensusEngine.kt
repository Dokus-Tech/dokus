package tech.dokus.features.ai.ensemble

import tech.dokus.domain.Money
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Layer 2: Consensus Engine
 *
 * Compares field-by-field extractions from multiple models and resolves conflicts.
 * This enables higher accuracy by leveraging disagreement as a signal.
 *
 * ## Conflict Resolution Strategy
 * 1. **Agreement**: Both models extracted the same value -> use it with high confidence
 * 2. **One missing**: Only one model extracted value -> use it with lower confidence
 * 3. **Disagreement**: Models disagree -> apply weight rules (PREFER_FAST/EXPERT/REQUIRE_MATCH)
 *
 * ## Amount Comparison
 * For monetary amounts, numeric equivalence is checked (e.g., "100.00" == "100").
 *
 * ## Critical Fields
 * Certain fields (totalAmount, IBAN, paymentReference) have stricter requirements
 * and generate CRITICAL conflicts when models disagree.
 */
class ConsensusEngine(
    private val defaultWeights: Map<String, ModelWeight> = DEFAULT_FIELD_WEIGHTS
) {
    private val logger = loggerFor()

    // =========================================================================
    // Invoice Consensus
    // =========================================================================

    /**
     * Merge two invoice extractions into a consensus result.
     */
    fun mergeInvoices(
        fastCandidate: ExtractedInvoiceData?,
        expertCandidate: ExtractedInvoiceData?
    ): ConsensusResult<ExtractedInvoiceData> {
        if (fastCandidate == null && expertCandidate == null) {
            return ConsensusResult.NoData
        }
        if (fastCandidate == null) {
            return ConsensusResult.SingleSource(expertCandidate!!, source = "expert")
        }
        if (expertCandidate == null) {
            return ConsensusResult.SingleSource(fastCandidate, source = "fast")
        }

        val conflicts = mutableListOf<FieldConflict>()
        val merged = mergeInvoiceFields(fastCandidate, expertCandidate, conflicts)

        logger.info("Invoice consensus: ${conflicts.size} conflicts detected")

        return if (conflicts.isEmpty()) {
            ConsensusResult.Unanimous(merged)
        } else {
            ConsensusResult.WithConflicts(merged, ConflictReport(conflicts))
        }
    }

    private fun mergeInvoiceFields(
        fast: ExtractedInvoiceData,
        expert: ExtractedInvoiceData,
        conflicts: MutableList<FieldConflict>
    ): ExtractedInvoiceData {
        return ExtractedInvoiceData(
            // Vendor info
            vendorName = resolveString("vendorName", fast.vendorName, expert.vendorName, conflicts),
            vendorVatNumber = resolveString("vendorVatNumber", fast.vendorVatNumber, expert.vendorVatNumber, conflicts),
            vendorAddress = resolveString("vendorAddress", fast.vendorAddress, expert.vendorAddress, conflicts),

            // Invoice details
            invoiceNumber = resolveString("invoiceNumber", fast.invoiceNumber, expert.invoiceNumber, conflicts),
            issueDate = resolveString("issueDate", fast.issueDate, expert.issueDate, conflicts),
            dueDate = resolveString("dueDate", fast.dueDate, expert.dueDate, conflicts),
            paymentTerms = resolveString("paymentTerms", fast.paymentTerms, expert.paymentTerms, conflicts),

            // Line items - prefer expert's more detailed extraction
            lineItems = expert.lineItems.ifEmpty { fast.lineItems },

            // Totals (critical fields)
            currency = resolveString("currency", fast.currency, expert.currency, conflicts),
            subtotal = resolveAmount("subtotal", fast.subtotal, expert.subtotal, conflicts),
            vatBreakdown = expert.vatBreakdown.ifEmpty { fast.vatBreakdown },
            totalVatAmount = resolveAmount("totalVatAmount", fast.totalVatAmount, expert.totalVatAmount, conflicts),
            totalAmount = resolveAmount("totalAmount", fast.totalAmount, expert.totalAmount, conflicts),

            // Payment info (critical fields)
            iban = resolveString("iban", fast.iban, expert.iban, conflicts),
            bic = resolveString("bic", fast.bic, expert.bic, conflicts),
            paymentReference = resolveString("paymentReference", fast.paymentReference, expert.paymentReference, conflicts),

            // Metadata - use higher confidence
            confidence = calculateMergedConfidence(fast.confidence, expert.confidence, conflicts.size),

            // Text - prefer expert's extraction
            extractedText = expert.extractedText ?: fast.extractedText,

            // Provenance - prefer expert
            provenance = expert.provenance ?: fast.provenance,

            // Credit note meta
            creditNoteMeta = expert.creditNoteMeta ?: fast.creditNoteMeta
        )
    }

    // =========================================================================
    // Bill Consensus
    // =========================================================================

    /**
     * Merge two bill extractions into a consensus result.
     */
    fun mergeBills(
        fastCandidate: ExtractedBillData?,
        expertCandidate: ExtractedBillData?
    ): ConsensusResult<ExtractedBillData> {
        if (fastCandidate == null && expertCandidate == null) {
            return ConsensusResult.NoData
        }
        if (fastCandidate == null) {
            return ConsensusResult.SingleSource(expertCandidate!!, source = "expert")
        }
        if (expertCandidate == null) {
            return ConsensusResult.SingleSource(fastCandidate, source = "fast")
        }

        val conflicts = mutableListOf<FieldConflict>()
        val merged = mergeBillFields(fastCandidate, expertCandidate, conflicts)

        logger.info("Bill consensus: ${conflicts.size} conflicts detected")

        return if (conflicts.isEmpty()) {
            ConsensusResult.Unanimous(merged)
        } else {
            ConsensusResult.WithConflicts(merged, ConflictReport(conflicts))
        }
    }

    private fun mergeBillFields(
        fast: ExtractedBillData,
        expert: ExtractedBillData,
        conflicts: MutableList<FieldConflict>
    ): ExtractedBillData {
        return ExtractedBillData(
            // Supplier info
            supplierName = resolveString("supplierName", fast.supplierName, expert.supplierName, conflicts),
            supplierVatNumber = resolveString("supplierVatNumber", fast.supplierVatNumber, expert.supplierVatNumber, conflicts),
            supplierAddress = resolveString("supplierAddress", fast.supplierAddress, expert.supplierAddress, conflicts),

            // Bill details
            invoiceNumber = resolveString("invoiceNumber", fast.invoiceNumber, expert.invoiceNumber, conflicts),
            issueDate = resolveString("issueDate", fast.issueDate, expert.issueDate, conflicts),
            dueDate = resolveString("dueDate", fast.dueDate, expert.dueDate, conflicts),

            // Amounts
            currency = resolveString("currency", fast.currency, expert.currency, conflicts),
            amount = resolveAmount("amount", fast.amount, expert.amount, conflicts),
            vatAmount = resolveAmount("vatAmount", fast.vatAmount, expert.vatAmount, conflicts),
            vatRate = resolveString("vatRate", fast.vatRate, expert.vatRate, conflicts),
            totalAmount = resolveAmount("totalAmount", fast.totalAmount, expert.totalAmount, conflicts),

            // Line items
            lineItems = expert.lineItems.ifEmpty { fast.lineItems },

            // Categorization
            category = resolveString("category", fast.category, expert.category, conflicts),
            description = resolveString("description", fast.description, expert.description, conflicts),

            // Payment
            paymentTerms = resolveString("paymentTerms", fast.paymentTerms, expert.paymentTerms, conflicts),
            bankAccount = resolveString("bankAccount", fast.bankAccount, expert.bankAccount, conflicts),

            notes = expert.notes ?: fast.notes,
            confidence = calculateMergedConfidence(fast.confidence, expert.confidence, conflicts.size),
            extractedText = expert.extractedText ?: fast.extractedText,
            provenance = expert.provenance ?: fast.provenance
        )
    }

    // =========================================================================
    // Expense Consensus
    // =========================================================================

    /**
     * Merge two expense extractions into a consensus result.
     */
    fun mergeExpenses(
        fastCandidate: ExtractedExpenseData?,
        expertCandidate: ExtractedExpenseData?
    ): ConsensusResult<ExtractedExpenseData> {
        if (fastCandidate == null && expertCandidate == null) {
            return ConsensusResult.NoData
        }
        if (fastCandidate == null) {
            return ConsensusResult.SingleSource(expertCandidate!!, source = "expert")
        }
        if (expertCandidate == null) {
            return ConsensusResult.SingleSource(fastCandidate, source = "fast")
        }

        val conflicts = mutableListOf<FieldConflict>()
        val merged = mergeExpenseFields(fastCandidate, expertCandidate, conflicts)

        return if (conflicts.isEmpty()) {
            ConsensusResult.Unanimous(merged)
        } else {
            ConsensusResult.WithConflicts(merged, ConflictReport(conflicts))
        }
    }

    private fun mergeExpenseFields(
        fast: ExtractedExpenseData,
        expert: ExtractedExpenseData,
        conflicts: MutableList<FieldConflict>
    ): ExtractedExpenseData {
        return ExtractedExpenseData(
            merchantName = resolveString("merchantName", fast.merchantName, expert.merchantName, conflicts),
            description = resolveString("description", fast.description, expert.description, conflicts),
            date = resolveString("date", fast.date, expert.date, conflicts),
            totalAmount = resolveAmount("totalAmount", fast.totalAmount, expert.totalAmount, conflicts),
            currency = resolveString("currency", fast.currency, expert.currency, conflicts),
            category = resolveString("category", fast.category, expert.category, conflicts),
            paymentMethod = resolveString("paymentMethod", fast.paymentMethod, expert.paymentMethod, conflicts),
            vatAmount = resolveAmount("vatAmount", fast.vatAmount, expert.vatAmount, conflicts),
            vatRate = resolveString("vatRate", fast.vatRate, expert.vatRate, conflicts),
            reference = resolveString("reference", fast.reference, expert.reference, conflicts),
            confidence = calculateMergedConfidence(fast.confidence, expert.confidence, conflicts.size),
            extractedText = expert.extractedText ?: fast.extractedText,
            provenance = expert.provenance ?: fast.provenance
        )
    }

    // =========================================================================
    // Receipt Consensus
    // =========================================================================

    /**
     * Merge two receipt extractions into a consensus result.
     */
    fun mergeReceipts(
        fastCandidate: ExtractedReceiptData?,
        expertCandidate: ExtractedReceiptData?
    ): ConsensusResult<ExtractedReceiptData> {
        if (fastCandidate == null && expertCandidate == null) {
            return ConsensusResult.NoData
        }
        if (fastCandidate == null) {
            return ConsensusResult.SingleSource(expertCandidate!!, source = "expert")
        }
        if (expertCandidate == null) {
            return ConsensusResult.SingleSource(fastCandidate, source = "fast")
        }

        val conflicts = mutableListOf<FieldConflict>()
        val merged = mergeReceiptFields(fastCandidate, expertCandidate, conflicts)

        return if (conflicts.isEmpty()) {
            ConsensusResult.Unanimous(merged)
        } else {
            ConsensusResult.WithConflicts(merged, ConflictReport(conflicts))
        }
    }

    private fun mergeReceiptFields(
        fast: ExtractedReceiptData,
        expert: ExtractedReceiptData,
        conflicts: MutableList<FieldConflict>
    ): ExtractedReceiptData {
        return ExtractedReceiptData(
            merchantName = resolveString("merchantName", fast.merchantName, expert.merchantName, conflicts),
            merchantAddress = resolveString("merchantAddress", fast.merchantAddress, expert.merchantAddress, conflicts),
            merchantVatNumber = resolveString("merchantVatNumber", fast.merchantVatNumber, expert.merchantVatNumber, conflicts),
            receiptNumber = resolveString("receiptNumber", fast.receiptNumber, expert.receiptNumber, conflicts),
            transactionDate = resolveString("transactionDate", fast.transactionDate, expert.transactionDate, conflicts),
            transactionTime = resolveString("transactionTime", fast.transactionTime, expert.transactionTime, conflicts),
            items = expert.items.ifEmpty { fast.items },
            currency = resolveString("currency", fast.currency, expert.currency, conflicts),
            subtotal = resolveAmount("subtotal", fast.subtotal, expert.subtotal, conflicts),
            vatAmount = resolveAmount("vatAmount", fast.vatAmount, expert.vatAmount, conflicts),
            totalAmount = resolveAmount("totalAmount", fast.totalAmount, expert.totalAmount, conflicts),
            paymentMethod = resolveString("paymentMethod", fast.paymentMethod, expert.paymentMethod, conflicts),
            cardLastFour = resolveString("cardLastFour", fast.cardLastFour, expert.cardLastFour, conflicts),
            suggestedCategory = resolveString("suggestedCategory", fast.suggestedCategory, expert.suggestedCategory, conflicts),
            confidence = calculateMergedConfidence(fast.confidence, expert.confidence, conflicts.size),
            extractedText = expert.extractedText ?: fast.extractedText,
            provenance = expert.provenance ?: fast.provenance
        )
    }

    // =========================================================================
    // Field Resolution Logic
    // =========================================================================

    /**
     * Resolve a string field between two model extractions.
     */
    private fun resolveString(
        field: String,
        fastValue: String?,
        expertValue: String?,
        conflicts: MutableList<FieldConflict>
    ): String? {
        // Both null or both equal
        if (fastValue == expertValue) return expertValue

        // Normalize for comparison (trim whitespace, lowercase for some comparisons)
        val normalizedFast = fastValue?.trim()
        val normalizedExpert = expertValue?.trim()

        if (normalizedFast == normalizedExpert) {
            return expertValue // Use expert's formatting
        }

        // One null: use the other
        if (fastValue == null) return expertValue
        if (expertValue == null) return fastValue

        // Both non-null and different: CONFLICT
        val weight = defaultWeights[field] ?: ModelWeight.PREFER_EXPERT
        val severity = if (CRITICAL_FIELDS.contains(field)) {
            ConflictSeverity.CRITICAL
        } else {
            ConflictSeverity.WARNING
        }

        val chosen = when (weight) {
            ModelWeight.PREFER_FAST -> fastValue
            ModelWeight.PREFER_EXPERT -> expertValue
            ModelWeight.REQUIRE_MATCH -> null // Force review
        }

        conflicts.add(
            FieldConflict(
                field = field,
                fastValue = fastValue,
                expertValue = expertValue,
                chosenValue = chosen,
                chosenSource = when {
                    chosen == fastValue -> "fast"
                    chosen == expertValue -> "expert"
                    else -> "none"
                },
                severity = severity
            )
        )

        return chosen
    }

    /**
     * Resolve a monetary amount field between two model extractions.
     * Also checks numeric equivalence (e.g., "100.00" == "100").
     */
    private fun resolveAmount(
        field: String,
        fastValue: String?,
        expertValue: String?,
        conflicts: MutableList<FieldConflict>
    ): String? {
        // Both null or both equal
        if (fastValue == expertValue) return expertValue

        // Check numeric equivalence
        val fastMoney = fastValue?.let { Money.parse(it) }
        val expertMoney = expertValue?.let { Money.parse(it) }

        if (fastMoney != null && expertMoney != null && fastMoney == expertMoney) {
            // Same amount, different format - use expert's format
            return expertValue
        }

        // Genuine conflict (or one is null)
        return resolveString(field, fastValue, expertValue, conflicts)
    }

    /**
     * Calculate merged confidence based on individual confidences and conflicts.
     */
    private fun calculateMergedConfidence(
        fastConfidence: Double,
        expertConfidence: Double,
        conflictCount: Int
    ): Double {
        // Base: average of both with expert weighted higher
        val baseConfidence = (fastConfidence + expertConfidence * 2) / 3

        // Penalty for conflicts (5% per conflict, max 25% reduction)
        val conflictPenalty = minOf(conflictCount * 0.05, 0.25)

        return maxOf(0.0, baseConfidence - conflictPenalty)
    }

    companion object {
        /**
         * Fields that are critical for correctness.
         * Conflicts on these fields have CRITICAL severity.
         */
        private val CRITICAL_FIELDS = setOf(
            "totalAmount",
            "subtotal",
            "totalVatAmount",
            "vatAmount",
            "amount",
            "iban",
            "paymentReference",
            "vendorVatNumber",
            "supplierVatNumber",
            "merchantVatNumber"
        )

        /**
         * Default field weights for conflict resolution.
         * Expert model is preferred for most fields.
         */
        private val DEFAULT_FIELD_WEIGHTS = mapOf(
            // Critical financial fields - prefer expert
            "totalAmount" to ModelWeight.PREFER_EXPERT,
            "subtotal" to ModelWeight.PREFER_EXPERT,
            "totalVatAmount" to ModelWeight.PREFER_EXPERT,
            "vatAmount" to ModelWeight.PREFER_EXPERT,
            "amount" to ModelWeight.PREFER_EXPERT,

            // Identifiers - prefer expert
            "vendorVatNumber" to ModelWeight.PREFER_EXPERT,
            "supplierVatNumber" to ModelWeight.PREFER_EXPERT,
            "merchantVatNumber" to ModelWeight.PREFER_EXPERT,
            "iban" to ModelWeight.PREFER_EXPERT,
            "paymentReference" to ModelWeight.PREFER_EXPERT,
            "invoiceNumber" to ModelWeight.PREFER_EXPERT,

            // Names - prefer expert
            "vendorName" to ModelWeight.PREFER_EXPERT,
            "supplierName" to ModelWeight.PREFER_EXPERT,
            "merchantName" to ModelWeight.PREFER_EXPERT,

            // Dates - prefer expert
            "issueDate" to ModelWeight.PREFER_EXPERT,
            "dueDate" to ModelWeight.PREFER_EXPERT,
            "date" to ModelWeight.PREFER_EXPERT,
            "transactionDate" to ModelWeight.PREFER_EXPERT
        )
    }
}
