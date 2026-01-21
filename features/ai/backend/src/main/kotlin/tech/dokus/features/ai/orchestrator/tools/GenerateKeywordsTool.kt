package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tool for generating search keywords from document extraction.
 *
 * Extracts keywords for full-text search indexing including:
 * - Vendor/supplier/merchant name
 * - Product/service descriptions
 * - Invoice/bill numbers
 * - VAT numbers
 * - Amounts
 * - Categories
 */
object GenerateKeywordsTool : SimpleTool<GenerateKeywordsTool.Args>(
    argsSerializer = Args.serializer(),
    name = "generate_keywords",
    description = """
        Generates search keywords from document extraction data.

        Extracts terms useful for full-text search:
        - Vendor/merchant names
        - Product/service descriptions
        - Document numbers
        - VAT numbers
        - Categories

        Returns a list of keywords separated by commas.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The extraction JSON to extract keywords from")
        val extraction: String
    )

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    // Words to exclude from keywords
    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "as", "is", "was", "are", "were", "been",
        "be", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "must", "shall", "can", "de", "en",
        "het", "van", "een", "voor", "met", "op", "te", "zijn", "wordt"
    )

    override suspend fun execute(args: Args): String {
        return try {
            val extraction = jsonFormat.parseToJsonElement(args.extraction).jsonObject
            val keywords = mutableSetOf<String>()

            // Extract vendor/merchant names
            extractKeywordsFromFields(
                extraction,
                keywords,
                "vendorName", "supplierName", "merchantName",
                "vendorAddress", "supplierAddress"
            )

            // Extract document numbers
            extractKeywordsFromFields(
                extraction,
                keywords,
                "invoiceNumber", "billNumber", "receiptNumber"
            )

            // Extract VAT numbers
            extractKeywordsFromFields(
                extraction,
                keywords,
                "vendorVatNumber", "supplierVatNumber", "vatNumber"
            )

            // Extract categories
            extractKeywordsFromFields(
                extraction,
                keywords,
                "expenseCategory", "category"
            )

            // Extract line item descriptions
            extractLineItemKeywords(extraction, keywords)

            // Clean and filter keywords
            val cleanedKeywords = keywords
                .flatMap { it.split(Regex("[\\s,;./\\-_]+")) }
                .map { it.lowercase().trim() }
                .filter { it.length >= 3 }
                .filter { it !in stopWords }
                .filter { !it.all { c -> c.isDigit() } }
                .distinct()
                .take(50)

            "SUCCESS: ${cleanedKeywords.joinToString(", ")}"
        } catch (e: Exception) {
            "ERROR: Failed to generate keywords: ${e.message}"
        }
    }

    private fun extractKeywordsFromFields(
        obj: Map<String, JsonElement>,
        keywords: MutableSet<String>,
        vararg fieldNames: String
    ) {
        for (name in fieldNames) {
            obj[name]?.let { element ->
                try {
                    val value = element.jsonPrimitive.content
                    if (value.isNotBlank()) {
                        keywords.add(value)
                    }
                } catch (_: Exception) {
                    // Skip non-primitive elements
                }
            }
        }
    }

    private fun extractLineItemKeywords(
        extraction: Map<String, JsonElement>,
        keywords: MutableSet<String>
    ) {
        val lineItems = extraction["lineItems"] ?: return

        try {
            val items: JsonArray = lineItems.jsonArray
            for (item in items.take(10)) { // Limit to first 10 items
                val itemObj = item.jsonObject
                itemObj["description"]?.let { desc ->
                    try {
                        keywords.add(desc.jsonPrimitive.content)
                    } catch (_: Exception) {
                        // Skip
                    }
                }
            }
        } catch (_: Exception) {
            // Not an array, try as single object
            try {
                val itemObj = lineItems.jsonObject
                itemObj["description"]?.let { desc ->
                    keywords.add(desc.jsonPrimitive.content)
                }
            } catch (_: Exception) {
                // Skip
            }
        }
    }
}
