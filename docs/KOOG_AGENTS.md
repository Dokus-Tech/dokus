# Koog AI Framework - Agent Implementation Guide

> Technical documentation for the Koog AI framework usage in Dokus.

## Overview

**Koog** is a JetBrains AI framework used for document processing and AI-powered features. It provides a unified API for prompt execution, LLM integration, and vision-based document analysis.

- **Library**: `ai.koog:koog-agents`
- **Version**: `0.6.0`
- **Location**: Declared in `/gradle/libs.versions.toml`

---

## 1. Core Components

### 1.1 PromptExecutor

The execution engine that sends prompts to LLMs and returns responses.

```kotlin
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor

// Create executor for Ollama
val executor: PromptExecutor = simpleOllamaAIExecutor("http://localhost:11434")

// Execute a prompt
val response = executor.execute(prompt, model, tools).first()
val content: String = response.content
```

### 1.2 LLModel

Defines an LLM's capabilities, context length, and identification.

```kotlin
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLMCapability

val visionModel = LLModel(
    provider = LLMProvider.Ollama,
    id = "qwen3-vl:32b",
    capabilities = listOf(LLMCapability.Vision.Image),
    contextLength = 131_072L,
    maxOutputTokens = null
)
```

### 1.3 Message Types

Structured conversation messages with content parts.

```kotlin
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock

// System message (instructions)
val systemMessage = Message.System(
    parts = listOf(ContentPart.Text("You are a document analyst...")),
    metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
)

// User message with text and images
val userMessage = Message.User(
    parts = listOf(
        ContentPart.Text("Analyze this document:"),
        ContentPart.Image(
            content = AttachmentContent.Binary.Bytes(imageBytes),
            format = "png",
            mimeType = "image/png"
        )
    ),
    metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
)
```

### 1.4 Prompt

Container for messages sent to the LLM.

```kotlin
import ai.koog.prompt.dsl.Prompt

val prompt = Prompt(
    messages = listOf(systemMessage, userMessage),
    id = "document-classifier"
)
```

### 1.5 AIAgent

High-level agentic layer for reasoning and tool use.

```kotlin
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry

val agent = AIAgent(
    promptExecutor = executor,
    llmModel = model,
    strategy = singleRunStrategy(),
    toolRegistry = ToolRegistry.EMPTY,
    id = "my-agent",
    systemPrompt = "You are..."
)

val response: String = agent.run("User query")
```

---

## 2. Model Configuration

### AIModels.kt

```kotlin
object AIModels {
    // Vision Models (document analysis)
    val VISION_LIGHT = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:2b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = 32_768L
    )

    val VISION_QUALITY = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3-vl:32b",
        capabilities = listOf(LLMCapability.Vision.Image),
        contextLength = 131_072L
    )

    // Chat Models (text-only)
    val CHAT_LIGHT = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3:8b",
        capabilities = emptyList(),
        contextLength = 32_768L
    )

    val CHAT_QUALITY = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen3:30b-a3b",
        capabilities = emptyList(),
        contextLength = 131_072L
    )

    // Embedding
    const val EMBEDDING_MODEL_NAME = "nomic-embed-text"
    const val EMBEDDING_DIMENSIONS = 768
}
```

### AIProviderFactory.kt

```kotlin
object AIProviderFactory {
    fun createExecutor(config: AIConfig): PromptExecutor {
        return simpleOllamaAIExecutor(config.ollamaHost)
    }

    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        return AIModels.forPurpose(config.mode, purpose)
    }
}
```

---

## 3. Agents

### 3.1 DocumentClassificationAgent

**Purpose**: Classify documents into types using vision models.

**Location**: `features/ai/backend/.../agents/DocumentClassificationAgent.kt`

```kotlin
class DocumentClassificationAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.DocumentClassification
) {
    suspend fun classify(
        images: List<DocumentImage>,
        tenantContext: AgentPrompt.TenantContext
    ): DocumentClassification {
        // Build system message with tenant context
        val systemMessage = Message.System(
            parts = listOf(ContentPart.Text(prompt.build(tenantContext))),
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        // Build user message with images
        val userParts = buildList {
            add(ContentPart.Text("Classify this ${images.size}-page document:"))
            images.forEach { docImage ->
                add(ContentPart.Image(
                    content = AttachmentContent.Binary.Bytes(docImage.imageBytes),
                    format = "png",
                    mimeType = docImage.mimeType
                ))
            }
        }
        val userMessage = Message.User(
            parts = userParts,
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        // Execute
        val visionPrompt = Prompt(
            messages = listOf(systemMessage, userMessage),
            id = "document-classifier"
        )
        val response = executor.execute(visionPrompt, model, emptyList()).first()

        return parseClassificationResponse(response.content)
    }
}
```

**Output**:

```kotlin
data class DocumentClassification(
    val documentType: ClassifiedDocumentType,
    val confidence: Double,
    val reasoning: String
)

enum class ClassifiedDocumentType {
    INVOICE, INBOUND_INVOICE, RECEIPT, CREDIT_NOTE, PRO_FORMA, EXPENSE, UNKNOWN
}
```

---

### 3.2 ExtractionAgent<T>

**Purpose**: Extract structured data from documents using vision models.

**Location**: `features/ai/backend/.../agents/ExtractionAgent.kt`

```kotlin
interface ExtractionAgent<T : Any> {
    suspend fun extract(images: List<DocumentImage>): T

    companion object {
        inline operator fun <reified T : Any> invoke(
            executor: PromptExecutor,
            model: LLModel,
            prompt: AgentPrompt.Extraction,
            userPromptPrefix: String,
            promptId: String,
            noinline emptyResult: () -> T
        ): ExtractionAgent<T>
    }
}
```

**Usage in AIService**:

```kotlin
private val invoiceAgent by lazy {
    ExtractionAgent<ExtractedInvoiceData>(
        executor = executor,
        model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION),
        prompt = AgentPrompt.Extraction.Invoice,
        userPromptPrefix = "Extract invoice data from this",
        promptId = "invoice-extractor",
        emptyResult = { ExtractedInvoiceData(confidence = 0.0) }
    )
}

private val inboundInvoiceAgent by lazy {
    ExtractionAgent<ExtractedInvoiceData>(
        executor = executor,
        model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION),
        prompt = AgentPrompt.Extraction.Inbound Invoice,
        userPromptPrefix = "Extract inbound invoice/supplier invoice data from this",
        promptId = "inbound invoice-extractor",
        emptyResult = { ExtractedInvoiceData(confidence = 0.0) }
    )
}

private val receiptAgent by lazy {
    ExtractionAgent<ExtractedReceiptData>(
        executor = executor,
        model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION),
        prompt = AgentPrompt.Extraction.Receipt,
        userPromptPrefix = "Extract receipt data from this",
        promptId = "receipt-extractor",
        emptyResult = { ExtractedReceiptData(confidence = 0.0) }
    )
}
```

**Data Classes**:

| Agent | Data Class | Use Case |
|-------|------------|----------|
| Invoice | `ExtractedInvoiceData` | Outbound invoices you sent |
| Inbound Invoice | `ExtractedInvoiceData` | Inbound invoices you received |
| Receipt | `ExtractedReceiptData` | POS receipts |
| Expense | `ExtractedExpenseData` | Simple expenses |
| CreditNote | `ExtractedInvoiceData` | Credit notes (reuses invoice schema) |
| ProForma | `ExtractedInvoiceData` | Quotes (reuses invoice schema) |

---

### 3.3 CategorySuggestionAgent

**Purpose**: Suggest expense categories for inbound invoices and receipts.

**Location**: `features/ai/backend/.../agents/CategorySuggestionAgent.kt`

```kotlin
class CategorySuggestionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.CategorySuggestion
) {
    suspend fun suggest(
        description: String,
        merchantName: String? = null
    ): CategorySuggestion {
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            strategy = singleRunStrategy(),
            toolRegistry = ToolRegistry.EMPTY,
            id = "category-suggester",
            systemPrompt = prompt.systemPrompt
        )

        val userPrompt = buildString {
            append("Categorize: $description")
            merchantName?.let { append("\nMerchant: $it") }
        }

        val response = agent.run(userPrompt)
        return parseSuggestionResponse(response)
    }
}
```

**Output**:

```kotlin
data class CategorySuggestion(
    val suggestedCategory: String,
    val confidence: Double,
    val reasoning: String,
    val alternativeCategories: List<Alternative> = emptyList()
)
```

**Categories** (Belgian tax-aligned):

```
OFFICE_SUPPLIES, HARDWARE, SOFTWARE, TRAVEL, TRANSPORTATION,
MEALS, PROFESSIONAL_SERVICES, UTILITIES, TRAINING, MARKETING,
INSURANCE, RENT, OTHER
```

---

### 3.4 ChatAgent

**Purpose**: RAG-powered Q&A over documents.

**Location**: `features/ai/backend/.../agents/ChatAgent.kt`

```kotlin
class ChatAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val ragService: RAGService,
    private val prompt: AgentPrompt
) {
    suspend fun chat(
        tenantId: TenantId,
        question: String,
        documentId: DocumentId? = null,
        conversationHistory: List<ConversationMessage>? = null
    ): ChatResponse {
        // Step 1: Retrieve relevant chunks via vector search
        val chunks = ragService.retrieveRelevantChunks(
            tenantId = tenantId,
            query = question,
            topK = DEFAULT_TOP_K
        )

        // Step 2: Assemble context from chunks
        val context = ragService.assembleContext(chunks, DEFAULT_MAX_CONTEXT_TOKENS)

        // Step 3: Build system prompt with RAG context
        val systemPrompt = ragService.formatRAGPrompt(prompt.systemPrompt, context)

        // Step 4: Create agent and run
        val agent = AIAgent(
            promptExecutor = executor,
            llmModel = model,
            strategy = singleRunStrategy(),
            toolRegistry = ToolRegistry.EMPTY,
            id = "chat-agent",
            systemPrompt = systemPrompt
        )

        val answer = agent.run(question)

        // Step 5: Extract citations
        val citations = chunks.map { chunk ->
            Citation(
                chunkId = chunk.id,
                documentId = chunk.documentId,
                excerpt = chunk.content.take(200),
                similarityScore = chunk.similarityScore
            )
        }

        return ChatResponse(
            answer = answer,
            citations = citations,
            chunksRetrieved = chunks.size,
            usedContext = chunks.isNotEmpty()
        )
    }

    companion object {
        const val DEFAULT_TOP_K = 5
        const val DEFAULT_MAX_CONTEXT_TOKENS = 2000
        const val DEFAULT_MIN_SIMILARITY = 0.3f
    }
}
```

**Output**:

```kotlin
data class ChatResponse(
    val answer: String,
    val citations: List<Citation>,
    val chunksRetrieved: Int,
    val usedContext: Boolean,
    val generationTimeMs: Long,
    val confidence: Float,
    val documentState: DocumentState?
)

data class Citation(
    val chunkId: String,
    val documentId: String,
    val documentName: String?,
    val pageNumber: Int?,
    val excerpt: String,
    val similarityScore: Float
)
```

---

## 4. Orchestration - AIService

**Location**: `features/ai/backend/.../service/AIService.kt`

```kotlin
class AIService(private val config: AIConfig) {

    // Lazy initialization
    private val executor by lazy {
        AIProviderFactory.createExecutor(config)
    }

    private val classificationAgent by lazy {
        DocumentClassificationAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.CLASSIFICATION),
            prompt = AgentPrompt.DocumentClassification
        )
    }

    private val invoiceAgent by lazy {
        ExtractionAgent<ExtractedInvoiceData>(...)
    }

    // Two-step processing
    suspend fun processDocument(
        images: List<DocumentImage>,
        tenantContext: AgentPrompt.TenantContext
    ): Result<DocumentAIResult> = runCatching {

        // Step 1: Classify
        val classification = classificationAgent.classify(images, tenantContext)

        // Step 2: Extract based on type
        when (classification.documentType) {
            ClassifiedDocumentType.INVOICE -> {
                val data = invoiceAgent.extract(images)
                DocumentAIResult.Invoice(classification, data)
            }
            ClassifiedDocumentType.INBOUND_INVOICE -> {
                val data = inboundInvoiceAgent.extract(images)
                DocumentAIResult.Inbound Invoice(classification, data)
            }
            // ... other types
        }
    }
}
```

---

## 5. Prompts - AgentPrompts

**Location**: `features/ai/backend/.../prompts/AgentPrompts.kt`

```kotlin
sealed class AgentPrompt {
    abstract val systemPrompt: String

    open fun build(context: TenantContext): String = systemPrompt

    data class TenantContext(
        val vatNumber: String?,
        val companyName: String?
    )

    // Classification prompt
    data object DocumentClassification : AgentPrompt() {
        override val systemPrompt: String = """
            You are a document classification expert...
        """.trimIndent()

        override fun build(context: TenantContext): String {
            return if (context.vatNumber != null || context.companyName != null) {
                systemPrompt + TENANT_CONTEXT_TEMPLATE.format(
                    context.vatNumber ?: "Unknown",
                    context.companyName ?: "Unknown"
                )
            } else {
                systemPrompt
            }
        }
    }

    // Extraction prompts
    sealed class Extraction : AgentPrompt() {
        data object Invoice : Extraction() { ... }
        data object Inbound Invoice : Extraction() { ... }
        data object Receipt : Extraction() { ... }
        data object CreditNote : Extraction() { ... }
        data object ProForma : Extraction() { ... }
        data object Expense : Extraction() { ... }
    }

    data object Chat : AgentPrompt() { ... }
    data object CategorySuggestion : AgentPrompt() { ... }

    companion object {
        fun extractionFor(documentType: ClassifiedDocumentType): Extraction
        fun extractionFor(documentType: String): Extraction
    }
}
```

---

## 6. Response Parsing

### JSON Normalization

```kotlin
// features/ai/backend/.../utils/MarkdownUtils.kt

internal fun normalizeJson(response: String): String {
    val cleaned = response
        .replace("```json", "")
        .replace("```", "")
        .trim()

    val startIndex = cleaned.indexOf('{')
    val endIndex = cleaned.lastIndexOf('}')

    return if (startIndex in 0..<endIndex) {
        cleaned.substring(startIndex, endIndex + 1)
    } else {
        cleaned
    }
}
```

### Safe Parsing Pattern

```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

private fun parseResponse(response: String): T {
    return try {
        val normalized = normalizeJson(response)
        json.decodeFromString(serializer(), normalized)
    } catch (e: Exception) {
        logger.warn("Parse failed: ${response.take(500)}", e)
        emptyResult()
    }
}
```

### Fallback Classification

```kotlin
private fun fallbackParse(response: String): DocumentClassification {
    val upperResponse = response.uppercase()
    val documentType = when {
        "INVOICE" in upperResponse -> ClassifiedDocumentType.INVOICE
        "RECEIPT" in upperResponse -> ClassifiedDocumentType.RECEIPT
        "INBOUND_INVOICE" in upperResponse -> ClassifiedDocumentType.INBOUND_INVOICE
        else -> ClassifiedDocumentType.UNKNOWN
    }
    return DocumentClassification(
        documentType = documentType,
        confidence = 0.5,  // Lower for fallback
        reasoning = "Fallback keyword detection"
    )
}
```

---

## 7. Vision Processing Pattern

### Complete Example

```kotlin
suspend fun classifyWithVision(images: List<DocumentImage>): DocumentClassification {
    // 1. Check for empty input
    if (images.isEmpty()) {
        return DocumentClassification(
            documentType = ClassifiedDocumentType.UNKNOWN,
            confidence = 0.0,
            reasoning = "No images provided"
        )
    }

    return try {
        // 2. Build system message
        val systemMessage = Message.System(
            parts = listOf(ContentPart.Text(prompt.systemPrompt)),
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        // 3. Build user message with images
        val userParts = buildList {
            add(ContentPart.Text("Classify this document:"))
            images.forEach { img ->
                add(ContentPart.Image(
                    content = AttachmentContent.Binary.Bytes(img.imageBytes),
                    format = "png",
                    mimeType = img.mimeType
                ))
            }
        }
        val userMessage = Message.User(
            parts = userParts,
            metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
        )

        // 4. Create prompt
        val visionPrompt = Prompt(
            messages = listOf(systemMessage, userMessage),
            id = "vision-classifier"
        )

        // 5. Execute
        val response = executor.execute(visionPrompt, model, emptyList()).first()

        // 6. Parse response
        parseClassificationResponse(response.content)

    } catch (e: Exception) {
        logger.error("Vision processing failed", e)
        DocumentClassification(
            documentType = ClassifiedDocumentType.UNKNOWN,
            confidence = 0.0,
            reasoning = "Processing failed: ${e.message}"
        )
    }
}
```

---

## 8. File Structure

```
features/ai/backend/src/main/kotlin/tech/dokus/features/ai/

├── agents/
│   ├── DocumentClassificationAgent.kt  # Vision: classify documents
│   ├── ExtractionAgent.kt              # Vision: extract structured data
│   ├── ChatAgent.kt                    # Text: RAG-powered Q&A
│   └── CategorySuggestionAgent.kt      # Text: expense categorization

├── config/
│   ├── AIProviderFactory.kt            # Executor and model creation
│   └── AIModels.kt                     # Model definitions

├── prompts/
│   └── AgentPrompts.kt                 # All agent prompts (sealed class)

├── service/
│   └── AIService.kt                    # High-level orchestration

├── services/
│   ├── RAGService.kt                   # Vector search and context assembly
│   ├── EmbeddingService.kt             # Generate embeddings
│   ├── ChunkingService.kt              # Text chunking
│   └── DocumentImageService.kt         # PDF to image conversion

├── utils/
│   └── MarkdownUtils.kt                # JSON normalization

└── models/
    ├── DocumentClassification.kt       # Classification result
    ├── ExtractedInvoiceData.kt         # Invoice extraction
    ├── ExtractedInvoiceData.kt            # Inbound Invoice extraction
    ├── ExtractedReceiptData.kt         # Receipt extraction
    ├── ExtractedExpenseData.kt         # Expense extraction
    ├── CategorySuggestion.kt           # Category suggestion
    └── DocumentAIResult.kt             # Combined result sealed class
```

---

## 9. Key Patterns

### Pattern 1: Lazy Initialization

```kotlin
private val executor by lazy {
    AIProviderFactory.createExecutor(config)
}

private val agent by lazy {
    ExtractionAgent<T>(executor, model, prompt, ...)
}
```

### Pattern 2: Error Handling with Fallback

```kotlin
return try {
    val response = executor.execute(prompt, model, emptyList()).first()
    parseResponse(response.content)
} catch (e: Exception) {
    logger.error("Failed", e)
    emptyResult()
}
```

### Pattern 3: Tenant Context Injection

```kotlin
fun build(context: TenantContext): String {
    return if (context.vatNumber != null) {
        systemPrompt + contextTemplate.format(context.vatNumber)
    } else {
        systemPrompt
    }
}
```

### Pattern 4: Two-Step Processing

```kotlin
// Step 1: Classify
val classification = classificationAgent.classify(images, context)

// Step 2: Extract based on type
val data = when (classification.documentType) {
    INVOICE -> invoiceAgent.extract(images)
    INBOUND_INVOICE -> inboundInvoiceAgent.extract(images)
    // ...
}
```

---

## 10. Configuration Summary

| Component | Configuration |
|-----------|--------------|
| Executor | `simpleOllamaAIExecutor(host)` |
| Vision Model (Light) | `qwen3-vl:2b` (32K context) |
| Vision Model (Quality) | `qwen3-vl:32b` (128K context) |
| Chat Model (Light) | `qwen3:8b` (32K context) |
| Chat Model (Quality) | `qwen3:30b-a3b` (128K context) |
| Embedding Model | `nomic-embed-text` (768 dims) |
| Mode Selection | `AIMode.LIGHT`, `NORMAL`, `CLOUD` |
