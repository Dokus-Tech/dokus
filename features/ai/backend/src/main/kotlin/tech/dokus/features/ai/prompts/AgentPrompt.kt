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
 * - Tenant context injection for direction resolution
 * - Provenance tracking for audit trail
 */
sealed class AgentPrompt {
    protected abstract val systemPrompt: Prompt

    /**
     * Build prompt without tenant context.
     * Use when tenant-specific customization isn't needed (e.g., chat, general queries).
     */
    open operator fun invoke(): Prompt = systemPrompt

    /**
     * Build prompt with tenant context.
     * Override in prompts that benefit from knowing user's company info.
     */
    open operator fun invoke(context: TenantContext): Prompt = systemPrompt

    /**
     * Tenant context for prompt customization.
     * Injecting user's VAT allows accurate direction detection for invoices.
     */
    data class TenantContext(
        val vatNumber: VatNumber,
        val companyName: LegalName,
        val address: Address
    ) {
        val prompt = TEMPLATE.format(
            vatNumber,
            companyName,
            "${address.streetLine1 ?: ""}, ${address.postalCode ?: ""} ${address.city ?: ""}, ${address.country ?: ""}"
                .replace(", ,", ",").trim(',', ' ')
        )

        companion object {

            private val TEMPLATE = Prompt(
                """
        ## Your Company Information
        Your company VAT number: %s
        Your company name: %s
        Company address: %s

        Use this to determine direction:
        - If YOUR VAT/name appears as sender → OUTBOUND invoice
        - If YOUR VAT/name appears as recipient → INBOUND invoice
        - If neither matches clearly, leave direction uncertain
    """
            )
        }
    }
}
