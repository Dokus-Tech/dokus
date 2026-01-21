package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tool for generating human-readable document descriptions.
 *
 * Creates a short description for UI display in the format:
 * "Vendor — Item/Service — €Amount — Month"
 *
 * Examples:
 * - "Road B.V. — EV Charging — €163.97 — November"
 * - "AWS — Cloud Hosting — $1,234.56 — January"
 * - "Office Depot — Office Supplies — €89.99 — March"
 */
object GenerateDescriptionTool : SimpleTool<GenerateDescriptionTool.Args>(
    argsSerializer = Args.serializer(),
    name = "generate_description",
    description = """
        Generates a human-readable description for a document.

        Creates a short, scannable description for UI display.
        Format: "Vendor — Item/Service — €Amount — Month"

        Input should be the extraction JSON from which vendor name,
        primary item/service, total amount, and date can be extracted.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The extraction JSON containing vendor, items, amount, and date")
        val extraction: String
    )

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        return try {
            val extraction = jsonFormat.parseToJsonElement(args.extraction).jsonObject

            // Extract vendor name (check multiple possible field names)
            val vendorName = extractField(extraction, "vendorName", "supplierName", "merchantName")
                ?: "Unknown Vendor"

            // Extract primary item/service description
            val itemDescription = extractItemDescription(extraction)

            // Extract total amount and currency
            val (amount, currency) = extractAmount(extraction)

            // Extract month from date
            val month = extractMonth(extraction)

            // Build description
            val description = buildString {
                append(vendorName.take(30))
                append(" — ")
                append(itemDescription.take(30))
                if (amount != null) {
                    append(" — ")
                    append(currency)
                    append(amount)
                }
                if (month != null) {
                    append(" — ")
                    append(month)
                }
            }

            "SUCCESS: $description"
        } catch (e: Exception) {
            "ERROR: Failed to generate description: ${e.message}"
        }
    }

    private fun extractField(obj: Map<String, JsonElement>, vararg fieldNames: String): String? {
        for (name in fieldNames) {
            obj[name]?.let { element ->
                val value = element.jsonPrimitive.content
                if (value.isNotBlank()) return value
            }
        }
        return null
    }

    private fun extractItemDescription(extraction: Map<String, JsonElement>): String {
        // Try to get first line item description
        val lineItems = extraction["lineItems"]
        if (lineItems != null) {
            try {
                val firstItem = lineItems.jsonObject
                val desc = firstItem["description"]?.jsonPrimitive?.content
                if (!desc.isNullOrBlank()) return desc
            } catch (_: Exception) {
                // Try as array
            }
        }

        // Fallback to expense category
        val category = extractField(extraction, "expenseCategory", "category")
        if (category != null) return category

        // Generic fallback
        return "Document"
    }

    private fun extractAmount(extraction: Map<String, JsonElement>): Pair<String?, String> {
        val amount = extractField(extraction, "totalAmount", "amount", "total")
        val currency = extractField(extraction, "currency") ?: "€"

        val currencySymbol = when (currency.uppercase()) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            else -> currency
        }

        return amount to currencySymbol
    }

    private fun extractMonth(extraction: Map<String, JsonElement>): String? {
        val dateStr = extractField(
            extraction,
            "issueDate",
            "billDate",
            "receiptDate",
            "expenseDate",
            "date"
        ) ?: return null

        // Try to parse ISO date (YYYY-MM-DD)
        return try {
            val month = dateStr.substring(5, 7).toInt()
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            monthNames.getOrNull(month - 1)
        } catch (_: Exception) {
            null
        }
    }
}
