package ai.dokus.foundation.domain.model.ai

import ai.dokus.foundation.domain.database.DbEnum


/**
 * Supported AI providers.
 */
enum class AIProvider(override val dbValue: String) : DbEnum {
    OLLAMA("ollama"),
    OPENAI("openai");


    companion object {
        fun fromDbValue(value: String): AIProvider =
            AIProvider.entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown AIProvider: $value")
    }
}