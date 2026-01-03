package tech.dokus.domain.model.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Supported AI providers.
 */
@Serializable
enum class AiProvider(override val dbValue: String) : DbEnum {
    @SerialName("ollama")
    Ollama("ollama"),

    @SerialName("openai")
    OpenAi("openai");

    companion object {
        fun fromDbValue(value: String): AiProvider =
            entries.find { it.dbValue == value }
                ?: throw IllegalArgumentException("Unknown AiProvider: $value")
    }
}
