package ai.dokus.ai.config

import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.OllamaModels
import org.slf4j.LoggerFactory

/**
 * Factory for creating AI prompt executors and models based on configuration.
 */
object AIProviderFactory {
    private val logger = LoggerFactory.getLogger(AIProviderFactory::class.java)

    /**
     * Create a prompt executor based on the configured provider.
     */
    fun createExecutor(config: AIConfig): PromptExecutor {
        return when (config.defaultProvider) {
            AIConfig.AIProvider.OLLAMA -> {
                logger.info("Creating Ollama executor: ${config.ollama.baseUrl}")
                simpleOllamaAIExecutor(config.ollama.baseUrl)
            }
            AIConfig.AIProvider.OPENAI -> {
                logger.info("Creating OpenAI executor")
                simpleOpenAIExecutor(config.openai.apiKey)
            }
        }
    }

    /**
     * Get the model for a specific purpose based on the configured provider.
     */
    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        val modelName = config.getModel(purpose)
        return when (config.defaultProvider) {
            AIConfig.AIProvider.OLLAMA -> createOllamaModel(modelName)
            AIConfig.AIProvider.OPENAI -> createOpenAIModel(modelName)
        }
    }

    /**
     * Create an Ollama model reference.
     * For models not predefined in Koog, creates a custom LLModel.
     */
    private fun createOllamaModel(modelName: String): LLModel {
        return when {
            // Meta Llama models
            modelName.startsWith("llama3.2") || modelName == "llama3:latest" -> OllamaModels.Meta.LLAMA_3_2
            modelName.startsWith("llama3.2:3b") -> OllamaModels.Meta.LLAMA_3_2_3B

            // Alibaba Qwen models
            modelName.startsWith("qwq") -> OllamaModels.Alibaba.QWQ
            modelName.startsWith("qwen2.5:0.5") -> OllamaModels.Alibaba.QWEN_2_5_05B
            modelName.startsWith("qwen3:0.6") -> OllamaModels.Alibaba.QWEN_3_06B

            // For all other models (including Mistral), create custom model
            else -> {
                logger.info("Creating custom Ollama model: $modelName")
                createCustomOllamaModel(modelName)
            }
        }
    }

    /**
     * Create a custom Ollama model for models not predefined in Koog.
     */
    private fun createCustomOllamaModel(modelName: String): LLModel {
        // Estimate context length based on model name
        val contextLength = when {
            modelName.contains("7b", ignoreCase = true) -> 32768L
            modelName.contains("8b", ignoreCase = true) -> 32768L
            modelName.contains("3b", ignoreCase = true) -> 8192L
            modelName.contains("13b", ignoreCase = true) -> 4096L
            else -> 32768L // Default
        }

        return LLModel(
            provider = LLMProvider.Ollama,
            id = modelName,
            capabilities = emptyList(),
            contextLength = contextLength,
            maxOutputTokens = null
        )
    }

    /**
     * Create an OpenAI model reference.
     */
    private fun createOpenAIModel(modelName: String): LLModel {
        return when (modelName) {
            "gpt-4o", "gpt4o" -> OpenAIModels.Chat.GPT4o
            "gpt-4o-mini", "gpt4o-mini" -> OpenAIModels.CostOptimized.GPT4oMini
            "gpt-4.1", "gpt-4-1" -> OpenAIModels.Chat.GPT4_1
            "gpt-4.1-mini" -> OpenAIModels.CostOptimized.GPT4_1Mini
            "gpt-4.1-nano" -> OpenAIModels.CostOptimized.GPT4_1Nano
            "gpt-5" -> OpenAIModels.Chat.GPT5
            "gpt-5-mini" -> OpenAIModels.Chat.GPT5Mini
            "o3-mini" -> OpenAIModels.CostOptimized.O3Mini
            "o4-mini" -> OpenAIModels.CostOptimized.O4Mini
            else -> {
                logger.warn("Unknown OpenAI model: $modelName, defaulting to GPT-4o-mini")
                OpenAIModels.CostOptimized.GPT4oMini
            }
        }
    }
}
