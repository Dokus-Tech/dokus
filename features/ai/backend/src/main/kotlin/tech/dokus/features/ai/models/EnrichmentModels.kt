package tech.dokus.features.ai.models

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.EnrichmentEntityType
import tech.dokus.domain.ids.TenantId
import java.util.UUID

/**
 * Input for the business enrichment AI graph.
 */
@Serializable
data class EnrichBusinessInput(
    val companyName: String,
    val vatNumber: String? = null,
    val country: String? = null,
    val entityType: EnrichmentEntityType,
    val entityId: String, // UUID as string for serialization
    val tenantId: String  // UUID as string for serialization
) {
    val prompt: String
        get() = buildString {
            appendLine("You are a business researcher. Your task is to find and verify the official website of a company, then extract key information from it.")
            appendLine()
            appendLine("## COMPANY TO RESEARCH")
            appendLine("- Company name: $companyName")
            if (!vatNumber.isNullOrBlank()) appendLine("- VAT number: $vatNumber")
            if (!country.isNullOrBlank()) appendLine("- Country: $country")
            appendLine()
            appendLine("## INSTRUCTIONS")
            appendLine()
            appendLine("### Step 1: Find the company website")
            appendLine("Use `searchCompanyWebsite` to search for the company's official website.")
            appendLine()
            appendLine("### Step 2: Verify the website is correct")
            appendLine("Before proceeding, verify the found website is actually for this company:")
            appendLine("- Does the domain name match or relate to the company name?")
            if (!vatNumber.isNullOrBlank()) {
                appendLine("- Does the website or search results mention the VAT number '$vatNumber'?")
            }
            appendLine("- Does the website description match the expected business?")
            appendLine("- If multiple results look plausible, scrape the top candidates to verify.")
            appendLine("- **Accuracy is critical**: Do NOT submit a website you're unsure about. Better to submit null than a wrong website.")
            appendLine()
            appendLine("### Step 3: Scrape the website")
            appendLine("Use `scrapeWebsite` to get the text content of the verified website.")
            appendLine("From the content, extract:")
            appendLine("- A concise 1-3 sentence summary of what the company does")
            appendLine("- A comma-separated list of business activities (e.g., 'Software Development, IT Consulting')")
            appendLine()
            appendLine("### Step 4: Extract logo")
            appendLine("Use `extractLogo` to find logo candidates on the website.")
            appendLine("Pick the best logo image URL:")
            appendLine("- Prefer PNG/JPEG from og:image or apple-touch-icon")
            appendLine("- Avoid SVG and ICO formats if better alternatives exist")
            appendLine("- If no good logo is found, submit null for logoUrl")
            appendLine()
            appendLine("### Step 5: Submit results")
            appendLine("Call `submit_enrichment` with all gathered information.")
            appendLine("Any field can be null if you couldn't find it with confidence.")
        }
}

/**
 * Output from the business enrichment AI graph.
 */
@Serializable
data class EnrichBusinessResult(
    val websiteUrl: String? = null,
    val summary: String? = null,
    val activities: String? = null,
    val logoUrl: String? = null
)
