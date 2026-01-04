package tech.dokus.features.ai.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.Base64

/**
 * Direct HTTP client for Ollama vision models.
 *
 * Bypasses Koog framework to avoid Kotlin version compatibility issues.
 * Uses Ollama's /api/chat endpoint with base64-encoded images.
 */
class OllamaVisionClient(
    private val baseUrl: String
) {
    private val logger = loggerFor()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        engine {
            requestTimeout = VISION_REQUEST_TIMEOUT_MS
        }
    }

    companion object {
        /** 5 minutes timeout for vision model processing */
        private const val VISION_REQUEST_TIMEOUT_MS = 300_000L
    }

    /**
     * Send a chat completion request with optional images.
     *
     * @param model The model name (e.g., "qwen3-vl:2b")
     * @param systemPrompt The system message
     * @param userPrompt The user message
     * @param images List of image byte arrays to include
     * @return The assistant's response content
     */
    suspend fun chat(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        images: List<ByteArray> = emptyList()
    ): String {
        val messages = buildList {
            // System message
            add(
                ChatMessage(
                    role = "system",
                    content = systemPrompt
                )
            )

            // User message with optional images
            add(
                ChatMessage(
                    role = "user",
                    content = userPrompt,
                    images = if (images.isNotEmpty()) {
                        images.map { Base64.getEncoder().encodeToString(it) }
                    } else {
                        null
                    }
                )
            )
        }

        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false
        )

        logger.debug("Sending chat request to Ollama: model=$model, images=${images.size}")

        val response = client.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ChatResponse>()

        return response.message.content
    }

    fun close() {
        client.close()
    }
}

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val options: ChatOptions? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

@Serializable
data class ChatOptions(
    val temperature: Double? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null
)

@Serializable
data class ChatResponse(
    val model: String,
    val message: AssistantMessage,
    @SerialName("done")
    val done: Boolean,
    @SerialName("total_duration")
    val totalDuration: Long? = null,
    @SerialName("load_duration")
    val loadDuration: Long? = null,
    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerialName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,
    @SerialName("eval_count")
    val evalCount: Int? = null,
    @SerialName("eval_duration")
    val evalDuration: Long? = null
)

@Serializable
data class AssistantMessage(
    val role: String,
    val content: String
)
