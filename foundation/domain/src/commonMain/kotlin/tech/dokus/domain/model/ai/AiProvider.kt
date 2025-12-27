package tech.dokus.domain.model.ai

import tech.dokus.domain.database.DbEnum


/**
 * Supported AI providers.
 */
enum class AIProvider(override val dbValue: String) : DbEnum {
    OLLAMA("ollama"),
    OPENAI("openai");


    companion object {
        fun fromDbValue(value: String): AIProvider =
            entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown AIProvider: $value")
    }
}