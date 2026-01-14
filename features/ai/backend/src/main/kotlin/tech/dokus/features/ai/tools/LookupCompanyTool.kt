package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.VatNumber
import tech.dokus.foundation.backend.lookup.CbeApiClient

/**
 * Layer 3 Tool: Belgian company lookup via CBE (Crossroads Bank for Enterprises).
 *
 * Looks up a company in the Belgian business registry to verify:
 * - The VAT number is valid and registered
 * - The company name matches official records
 * - The company is still active
 *
 * @param cbeApiClient The CBE API client for company lookups
 */
class LookupCompanyTool(
    private val cbeApiClient: CbeApiClient
) : SimpleTool<LookupCompanyTool.Args>(
    argsSerializer = Args.serializer(),
    name = "lookup_company",
    description = """
        Looks up a company in the Belgian CBE (Crossroads Bank for Enterprises) registry.

        Use this to verify:
        - A VAT number exists in the official Belgian registry
        - The extracted company name matches the official legal name
        - The company is still active

        Input: Belgian VAT number (e.g., 'BE0123456789' or '0123.456.789')
        Returns: Official company name, address, and status, or "NOT FOUND".
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "The Belgian VAT number to look up. " +
                "Can include 'BE' prefix, spaces, or dots. " +
                "Examples: 'BE0123456789', '0123.456.789', 'BE 0123 456 789'"
        )
        val vatNumber: String
    )

    override suspend fun execute(args: Args): String {
        // Create and normalize the VAT number
        var vatNumber = VatNumber(args.vatNumber)

        // Check if input looks valid enough to process
        if (vatNumber.normalized.isBlank()) {
            return "ERROR: Could not parse VAT number '${args.vatNumber}'. " +
                "Expected Belgian format like 'BE0123456789' or '0123.456.789'."
        }

        // Check country code
        val countryCode = vatNumber.countryCode
        when {
            // Explicit non-Belgian country code
            countryCode != null && countryCode != "BE" -> {
                return "NOT FOUND: CBE only contains Belgian companies. " +
                    "'${args.vatNumber}' appears to be from $countryCode."
            }
            // No country code - assume Belgian and add BE prefix
            countryCode == null -> {
                vatNumber = VatNumber.fromCountryAndCompanyNumber("BE", vatNumber.normalized)
            }
            // Already has BE prefix - good to go
        }

        // Look up in CBE
        return cbeApiClient.searchByVat(vatNumber).fold(
            onSuccess = { entity ->
                buildString {
                    appendLine("FOUND: Company registered in Belgian CBE registry.")
                    appendLine()
                    appendLine("Legal Name: ${entity.name.value}")
                    appendLine("VAT Number: ${entity.vatNumber?.normalized ?: "N/A"}")
                    appendLine("Enterprise Number: ${entity.enterpriseNumber}")
                    appendLine("Status: ${entity.status.name}")
                    entity.address?.let { addr ->
                        appendLine("Address: ${addr.streetLine1}, ${addr.postalCode} ${addr.city}")
                    }
                    appendLine()
                    appendLine("Use this official name for consistency in the extracted data.")
                }
            },
            onFailure = { error ->
                "NOT FOUND: VAT number '${args.vatNumber}' not found in Belgian CBE registry. " +
                    "Either the VAT number is incorrect, or this is a foreign company. " +
                    "Re-read the VAT number from the document header. " +
                    "Error: ${error.message}"
            }
        )
    }
}
