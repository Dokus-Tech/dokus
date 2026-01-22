package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import tech.dokus.domain.utils.parseSafe
import tech.dokus.features.ai.models.CategorySuggestion
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for suggesting expense categories.
 * Used for auto-categorization of expenses and bills.
 */
class CategorySuggestionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.CategorySuggestion
) {
    private val logger = loggerFor()

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
                systemPrompt = prompt.systemPrompt.value
            )

            val response: String = agent.run(userPrompt)
            parseSuggestionResponse(response)
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
        return parseSafe<CategorySuggestion>(normalizeJson(response)).getOrElse {
            logger.warn("Failed to parse suggestion response: ${response.take(500)}", it)
            fallbackSuggestion(response)
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
