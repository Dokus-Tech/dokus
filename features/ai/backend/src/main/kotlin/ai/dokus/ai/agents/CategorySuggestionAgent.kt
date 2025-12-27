package ai.dokus.ai.agents

import ai.dokus.ai.models.CategorySuggestion
import ai.dokus.foundation.ktor.utils.loggerFor
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json

/**
 * Agent responsible for suggesting expense categories.
 * Used for auto-categorization of expenses and bills.
 */
class CategorySuggestionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val systemPrompt = """
        You are an expense categorization specialist for Belgian IT freelancers.
        Suggest the most appropriate expense category based on the description.

        Available categories (Belgian tax-relevant):
        - OFFICE_SUPPLIES: Office equipment, stationery, desk accessories
        - HARDWARE: Computers, monitors, peripherals, electronic devices
        - SOFTWARE: Software licenses, SaaS subscriptions, cloud services
        - TRAVEL: Business travel, accommodation, flights, trains
        - TRANSPORTATION: Local transport, fuel, parking, car expenses
        - MEALS: Business meals, client entertainment
        - PROFESSIONAL_SERVICES: Legal, accounting, consulting fees
        - UTILITIES: Internet, phone, electricity (home office portion)
        - TRAINING: Courses, conferences, certifications, books
        - MARKETING: Advertising, website hosting, domain names
        - INSURANCE: Professional liability, health insurance
        - RENT: Office space, coworking memberships
        - OTHER: Miscellaneous business expenses

        Guidelines for Belgian IT freelancers:
        - Hardware > 500 EUR may need depreciation
        - Meals are typically 69% deductible
        - Home office utilities are partially deductible based on usage
        - Professional training is fully deductible

        Respond with a JSON object:
        {
            "suggestedCategory": "CATEGORY_NAME",
            "confidence": 0.0 to 1.0,
            "reasoning": "Brief explanation",
            "alternativeCategories": [
                {"category": "ALTERNATIVE", "confidence": 0.0 to 1.0}
            ]
        }
    """.trimIndent()

    /**
     * Suggest a category for an expense description.
     */
    suspend fun suggest(description: String, merchantName: String? = null): CategorySuggestion {
        logger.debug("Suggesting category for: $description")

        val context = buildString {
            append("Description: $description")
            if (merchantName != null) {
                append("\nMerchant: $merchantName")
            }
        }

        val userPrompt = """
            Suggest an expense category for:

            $context
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "category-suggester",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            parseSuggestionResponse(response ?: "")
        } catch (e: Exception) {
            logger.error("Failed to suggest category", e)
            CategorySuggestion(
                suggestedCategory = "OTHER",
                confidence = 0.0,
                reasoning = "Categorization failed: ${e.message}"
            )
        }
    }

    private fun parseSuggestionResponse(response: String): CategorySuggestion {
        return try {
            val jsonString = extractJson(response)
            json.decodeFromString<CategorySuggestion>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse suggestion response: ${response.take(500)}", e)
            fallbackSuggestion(response)
        }
    }

    private fun extractJson(response: String): String {
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }

    private fun fallbackSuggestion(response: String): CategorySuggestion {
        val upperResponse = response.uppercase()
        val category = CATEGORIES.find { it in upperResponse } ?: "OTHER"

        return CategorySuggestion(
            suggestedCategory = category,
            confidence = 0.5,
            reasoning = "Fallback categorization based on keyword detection"
        )
    }

    companion object {
        private val CATEGORIES = listOf(
            "OFFICE_SUPPLIES",
            "HARDWARE",
            "SOFTWARE",
            "TRAVEL",
            "TRANSPORTATION",
            "MEALS",
            "PROFESSIONAL_SERVICES",
            "UTILITIES",
            "TRAINING",
            "MARKETING",
            "INSURANCE",
            "RENT",
            "OTHER"
        )
    }
}
