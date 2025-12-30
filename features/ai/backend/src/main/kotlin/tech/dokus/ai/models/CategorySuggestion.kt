package tech.dokus.ai.models

import kotlinx.serialization.Serializable

/**
 * Category suggestion result from AI.
 */
@Serializable
data class CategorySuggestion(
    val suggestedCategory: String,
    val confidence: Double,
    val reasoning: String,
    val alternativeCategories: List<AlternativeCategory> = emptyList()
)

/**
 * An alternative category suggestion with lower confidence.
 */
@Serializable
data class AlternativeCategory(
    val category: String,
    val confidence: Double
)
