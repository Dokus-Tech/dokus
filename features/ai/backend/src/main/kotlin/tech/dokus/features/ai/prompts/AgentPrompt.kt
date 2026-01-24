package tech.dokus.features.ai.prompts

import tech.dokus.domain.LegalName
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address

/**
 * Agent prompts optimized for CORRECTNESS on Belgian business documents.
 *
 * Key features:
 * - Chain-of-thought reasoning for classification
 * - Few-shot examples with Belgian document formats
 * - Explicit validation rules (VAT, IBAN, dates, amounts)
 * - Tenant context injection for INVOICE/BILL distinction
 * - Provenance tracking for audit trail
 */
sealed class AgentPrompt {
    internal abstract val systemPrompt: Prompt

    /**
     * Build prompt with tenant context.
     * Override in prompts that benefit from knowing user's company info.
     */
    open operator fun invoke(context: TenantContext): Prompt = systemPrompt

    /**
     * Tenant context for prompt customization.
     * Injecting user's VAT allows accurate INVOICE vs BILL classification.
     */
    data class TenantContext(
        val vatNumber: VatNumber,
        val companyName: LegalName,
        val address: Address
    )
}
