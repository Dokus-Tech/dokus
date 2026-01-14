package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.ToolRegistry
import tech.dokus.foundation.backend.lookup.CbeApiClient

/**
 * Creates a ToolRegistry with all financial validation tools.
 *
 * These tools allow AI agents to validate extracted financial data:
 * - VerifyTotalsTool: Math verification (subtotal + VAT = total)
 * - ValidateOgmTool: Belgian OGM checksum validation
 * - ValidateIbanTool: IBAN checksum validation
 * - LookupCompanyTool: Belgian company registry lookup
 *
 * ## Usage
 * ```kotlin
 * val toolRegistry = FinancialToolRegistry.create(cbeApiClient)
 * val agent = AIAgent(
 *     ...,
 *     toolRegistry = toolRegistry
 * )
 * ```
 *
 * ## Tool Calling Considerations
 * Note: Ollama models may apply tools inconsistently. If tool calling
 * proves unreliable, validation can be performed as a post-processing
 * step instead of relying on the model to call tools.
 */
object FinancialToolRegistry {

    /**
     * Create a ToolRegistry with all financial validation tools.
     *
     * @param cbeApiClient Optional CBE API client for company lookups.
     *                     If null, the LookupCompanyTool will not be included.
     */
    fun create(cbeApiClient: CbeApiClient? = null): ToolRegistry {
        return ToolRegistry {
            tool(VerifyTotalsTool)
            tool(ValidateOgmTool)
            tool(ValidateIbanTool)
            if (cbeApiClient != null) {
                tool(LookupCompanyTool(cbeApiClient))
            }
        }
    }

    /**
     * Create a minimal registry with only validation tools (no external API calls).
     * Useful for testing or when CBE API is not available.
     */
    fun createValidationOnly(): ToolRegistry {
        return ToolRegistry {
            tool(VerifyTotalsTool)
            tool(ValidateOgmTool)
            tool(ValidateIbanTool)
        }
    }
}
