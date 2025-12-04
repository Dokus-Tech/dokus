# AI Integration Plan - Phase 2 & 3

## Overview

Integrate [JetBrains Koog AI Framework](https://github.com/JetBrains/koog) with configurable providers (Ollama for local/self-hosted, OpenAI for cloud) to power:
- Document OCR and data extraction
- Auto-categorization of expenses
- Smart suggestions for invoice fields

---

## Phase 2: Koog AI Integration

### 2.1 Module Structure

```
foundation/
├── ai/                              # NEW MODULE
│   ├── build.gradle.kts
│   └── src/main/kotlin/ai/dokus/foundation/ai/
│       ├── config/
│       │   ├── AIConfig.kt          # Configuration data class
│       │   └── AIProviderFactory.kt # Creates executors based on config
│       ├── agents/
│       │   ├── DocumentClassificationAgent.kt  # Step 1: Classify document type
│       │   ├── InvoiceExtractionAgent.kt       # Step 2a: Extract invoice data
│       │   ├── ReceiptExtractionAgent.kt       # Step 2b: Extract receipt data
│       │   ├── CategorySuggestionAgent.kt
│       │   └── SmartSuggestionAgent.kt
│       ├── tools/
│       │   ├── DocumentOCRTool.kt
│       │   ├── VATCalculatorTool.kt
│       │   └── CategoryLookupTool.kt
│       ├── models/
│       │   ├── DocumentClassification.kt   # Classification result
│       │   ├── ExtractedInvoiceData.kt
│       │   ├── ExtractedReceiptData.kt
│       │   └── CategorySuggestion.kt
│       └── service/
│           └── AIService.kt         # High-level API for services
```

### 2.2 Configuration Design

**AIConfig.kt** - Configuration data class:
```kotlin
data class AIConfig(
    val defaultProvider: AIProvider,  // OLLAMA or OPENAI
    val ollama: OllamaConfig,
    val openai: OpenAIConfig,
    val models: ModelConfig
) {
    data class OllamaConfig(
        val enabled: Boolean,
        val baseUrl: String,           // http://ollama:11434
        val defaultModel: String       // mistral:7b
    )

    data class OpenAIConfig(
        val enabled: Boolean,
        val apiKey: String,
        val defaultModel: String       // gpt-4o-mini
    )

    data class ModelConfig(
        val documentExtraction: String,  // Model for OCR/extraction
        val categorization: String,      // Model for categorization
        val suggestions: String          // Model for smart suggestions
    )

    enum class AIProvider { OLLAMA, OPENAI }

    companion object {
        fun fromConfig(config: Config): AIConfig = ...
    }
}
```

**application.conf** additions:
```hocon
ai {
    default-provider = "ollama"  # or "openai"
    default-provider = ${?AI_DEFAULT_PROVIDER}

    ollama {
        enabled = true
        enabled = ${?AI_OLLAMA_ENABLED}
        base-url = "http://localhost:11434"
        base-url = ${?AI_OLLAMA_URL}
        default-model = "mistral:7b"
        default-model = ${?AI_OLLAMA_MODEL}
    }

    openai {
        enabled = false
        enabled = ${?AI_OPENAI_ENABLED}
        api-key = ""
        api-key = ${?OPENAI_API_KEY}
        default-model = "gpt-4o-mini"
        default-model = ${?AI_OPENAI_MODEL}
    }

    models {
        document-extraction = ${ai.${ai.default-provider}.default-model}
        categorization = ${ai.${ai.default-provider}.default-model}
        suggestions = ${ai.${ai.default-provider}.default-model}
    }
}
```

### 2.3 Dependencies

**foundation/ai/build.gradle.kts**:
```kotlin
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Koog AI Framework
    implementation("ai.koog:koog-agents:0.5.4")

    // Foundation modules
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)

    // Serialization
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutinesCore)
}
```

**libs.versions.toml** additions:
```toml
[versions]
koog = "0.5.4"

[libraries]
koog-agents = { module = "ai.koog:koog-agents", version.ref = "koog" }
```

### 2.4 Core Implementation

**AIProviderFactory.kt**:
```kotlin
object AIProviderFactory {
    fun createExecutor(config: AIConfig): PromptExecutor {
        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> simpleOllamaAIExecutor(
                baseUrl = config.ollama.baseUrl
            )
            AIProvider.OPENAI -> simpleOpenAIAIExecutor(
                apiKey = config.openai.apiKey
            )
        }
    }

    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        val modelName = when (purpose) {
            ModelPurpose.DOCUMENT_EXTRACTION -> config.models.documentExtraction
            ModelPurpose.CATEGORIZATION -> config.models.categorization
            ModelPurpose.SUGGESTIONS -> config.models.suggestions
        }

        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> createOllamaModel(modelName)
            AIProvider.OPENAI -> createOpenAIModel(modelName)
        }
    }
}
```

**DocumentClassificationAgent.kt** - Step 1: Classify document type:
```kotlin
class DocumentClassificationAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = text {
            + """You are a document classification specialist.
                |Analyze the provided text and determine the document type.
                |
                |Document types:
                |- INVOICE: A formal request for payment from a supplier/vendor with invoice number, line items, VAT
                |- RECEIPT: A proof of payment/purchase from a store, usually simpler format
                |- BILL: A utility or service bill (electricity, phone, internet)
                |- UNKNOWN: Cannot determine the type
                |
                |Also assess confidence (0.0 to 1.0) based on how clear the classification is.""".trimMargin()
        }
    )

    suspend fun classify(ocrText: String): DocumentClassification {
        return agent.runWithStructuredOutput<DocumentClassification>(
            prompt = "Classify this document:\n\n$ocrText"
        )
    }
}

@Serializable
data class DocumentClassification(
    val documentType: ClassifiedDocumentType,
    val confidence: Double,
    val reasoning: String  // Brief explanation of classification
)

@Serializable
enum class ClassifiedDocumentType {
    INVOICE,    // Formal supplier invoice with line items
    RECEIPT,    // Store receipt / proof of purchase
    BILL,       // Utility/service bill
    UNKNOWN
}
```

**InvoiceExtractionAgent.kt** - Step 2a: Extract invoice-specific data:
```kotlin
class InvoiceExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = text {
            + """You are an invoice data extraction specialist.
                |Extract structured data from business invoices.
                |Always respond with valid JSON matching the requested schema.
                |
                |Extract these fields:
                |- Vendor: name, VAT number, address
                |- Invoice: number, issue date, due date, payment terms
                |- Line items: description, quantity, unit price, VAT rate, total
                |- Totals: subtotal, VAT breakdown, total amount, currency
                |- Payment: bank account (IBAN/BIC), payment reference
                |
                |Use null for fields that cannot be found.
                |Dates should be in ISO format (YYYY-MM-DD).""".trimMargin()
        }
    )

    suspend fun extract(ocrText: String): ExtractedInvoiceData {
        return agent.runWithStructuredOutput<ExtractedInvoiceData>(
            prompt = "Extract invoice data from this text:\n\n$ocrText"
        )
    }
}
```

**ReceiptExtractionAgent.kt** - Step 2b: Extract receipt-specific data:
```kotlin
class ReceiptExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val agent = AIAgent(
        promptExecutor = executor,
        llmModel = model,
        systemPrompt = text {
            + """You are a receipt data extraction specialist.
                |Extract structured data from store receipts and purchase proofs.
                |Always respond with valid JSON matching the requested schema.
                |
                |Extract these fields:
                |- Merchant: name, address, VAT number (if present)
                |- Transaction: date, time, receipt number
                |- Items: description, quantity, price (group similar items)
                |- Totals: subtotal, VAT amount, total, payment method
                |- Category suggestion based on merchant/items
                |
                |Use null for fields that cannot be found.
                |Dates should be in ISO format (YYYY-MM-DD).""".trimMargin()
        }
    )

    suspend fun extract(ocrText: String): ExtractedReceiptData {
        return agent.runWithStructuredOutput<ExtractedReceiptData>(
            prompt = "Extract receipt data from this text:\n\n$ocrText"
        )
    }
}
```

**AIService.kt** - High-level service API with two-step processing:
```kotlin
class AIService(
    private val config: AIConfig
) {
    private val executor = AIProviderFactory.createExecutor(config)

    // Step 1: Classification agent
    private val classificationAgent by lazy {
        DocumentClassificationAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.CLASSIFICATION)
        )
    }

    // Step 2: Type-specific extraction agents
    private val invoiceAgent by lazy {
        InvoiceExtractionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION)
        )
    }

    private val receiptAgent by lazy {
        ReceiptExtractionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION)
        )
    }

    private val categoryAgent by lazy {
        CategorySuggestionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.CATEGORIZATION)
        )
    }

    /**
     * Two-step document processing:
     * 1. Classify the document type
     * 2. Extract data using the appropriate agent
     */
    suspend fun processDocument(ocrText: String): Result<DocumentProcessingResult> = runCatching {
        // Step 1: Classify document type
        val classification = classificationAgent.classify(ocrText)

        // Step 2: Extract based on classification
        val extractedData: ExtractedDocumentData = when (classification.documentType) {
            ClassifiedDocumentType.INVOICE -> {
                val data = invoiceAgent.extract(ocrText)
                ExtractedDocumentData.Invoice(data)
            }
            ClassifiedDocumentType.RECEIPT -> {
                val data = receiptAgent.extract(ocrText)
                ExtractedDocumentData.Receipt(data)
            }
            ClassifiedDocumentType.BILL -> {
                // Bills use invoice extraction (similar structure)
                val data = invoiceAgent.extract(ocrText)
                ExtractedDocumentData.Bill(data)
            }
            ClassifiedDocumentType.UNKNOWN -> {
                throw IllegalArgumentException("Could not classify document type")
            }
        }

        DocumentProcessingResult(
            classification = classification,
            extractedData = extractedData
        )
    }

    /**
     * Direct invoice extraction (skip classification)
     */
    suspend fun extractInvoice(ocrText: String): Result<ExtractedInvoiceData> =
        runCatching { invoiceAgent.extract(ocrText) }

    /**
     * Direct receipt extraction (skip classification)
     */
    suspend fun extractReceipt(ocrText: String): Result<ExtractedReceiptData> =
        runCatching { receiptAgent.extract(ocrText) }

    /**
     * Suggest expense category
     */
    suspend fun suggestCategory(description: String): Result<CategorySuggestion> =
        runCatching { categoryAgent.suggest(description) }
}

/**
 * Result of two-step document processing
 */
data class DocumentProcessingResult(
    val classification: DocumentClassification,
    val extractedData: ExtractedDocumentData
)

/**
 * Sealed class for type-safe extracted data
 */
sealed class ExtractedDocumentData {
    data class Invoice(val data: ExtractedInvoiceData) : ExtractedDocumentData()
    data class Receipt(val data: ExtractedReceiptData) : ExtractedDocumentData()
    data class Bill(val data: ExtractedInvoiceData) : ExtractedDocumentData()  // Bills use invoice structure
}
```

### 2.5 Integration with Processor Service

Update **processor** service to use AIService with two-step processing:

```kotlin
// In processor DI
single { AIService(get<AIConfig>()) }

// In DocumentProcessor
class DocumentProcessor(
    private val aiService: AIService,
    private val documentRepo: ProcessorDocumentProcessingRepository,
    private val storage: ObjectStorage
) {
    suspend fun processDocument(item: ProcessingItem): Result<ProcessedDocument> {
        // 1. Get document from storage
        val documentBytes = storage.get(item.storageKey)

        // 2. Perform OCR (using existing OCR or AI vision)
        val ocrText = performOCR(documentBytes, item.contentType)

        // 3. Two-step AI processing: classify then extract
        val result = aiService.processDocument(ocrText)

        // 4. Update processing record
        return result.map { processingResult ->
            val (documentType, extractedData, confidence) = when (val data = processingResult.extractedData) {
                is ExtractedDocumentData.Invoice -> Triple(
                    DocumentType.Invoice,
                    data.data.toExtractedDocumentData(),
                    data.data.confidence
                )
                is ExtractedDocumentData.Receipt -> Triple(
                    DocumentType.Expense,
                    data.data.toExtractedDocumentData(),
                    data.data.confidence
                )
                is ExtractedDocumentData.Bill -> Triple(
                    DocumentType.Bill,
                    data.data.toExtractedDocumentData(),
                    data.data.confidence
                )
            }

            documentRepo.markAsProcessed(
                processingId = item.processingId,
                documentType = documentType,
                extractedData = extractedData,
                confidence = confidence,
                rawText = ocrText
            )

            ProcessedDocument(
                documentId = item.documentId,
                classification = processingResult.classification,
                extractedData = processingResult.extractedData
            )
        }
    }
}
```

### 2.6 Structured Output Models

**ExtractedInvoiceData.kt**:
```kotlin
@Serializable
data class ExtractedInvoiceData(
    // Vendor information
    val vendorName: String?,
    val vendorVatNumber: String?,
    val vendorAddress: String?,

    // Invoice details
    val invoiceNumber: String?,
    val issueDate: String?,           // ISO format YYYY-MM-DD
    val dueDate: String?,
    val paymentTerms: String?,        // e.g., "Net 30"

    // Line items
    val lineItems: List<InvoiceLineItem>,

    // Totals
    val currency: String?,            // EUR, USD, etc.
    val subtotal: String?,
    val vatBreakdown: List<VatBreakdown>?,
    val totalVatAmount: String?,
    val totalAmount: String?,

    // Payment information
    val iban: String?,
    val bic: String?,
    val paymentReference: String?,

    // Metadata
    val confidence: Double
) {
    @Serializable
    data class InvoiceLineItem(
        val description: String,
        val quantity: Double?,
        val unitPrice: String?,
        val vatRate: String?,         // e.g., "21%"
        val total: String?
    )

    @Serializable
    data class VatBreakdown(
        val rate: String,             // e.g., "21%"
        val base: String?,
        val amount: String?
    )
}
```

**ExtractedReceiptData.kt**:
```kotlin
@Serializable
data class ExtractedReceiptData(
    // Merchant information
    val merchantName: String?,
    val merchantAddress: String?,
    val merchantVatNumber: String?,

    // Transaction details
    val receiptNumber: String?,
    val transactionDate: String?,     // ISO format YYYY-MM-DD
    val transactionTime: String?,     // HH:mm format

    // Items (simplified compared to invoices)
    val items: List<ReceiptItem>,

    // Totals
    val currency: String?,
    val subtotal: String?,
    val vatAmount: String?,
    val totalAmount: String?,

    // Payment
    val paymentMethod: String?,       // Cash, Card, etc.
    val cardLastFour: String?,        // Last 4 digits if card payment

    // Category suggestion
    val suggestedCategory: String?,   // Based on merchant/items

    // Metadata
    val confidence: Double
) {
    @Serializable
    data class ReceiptItem(
        val description: String,
        val quantity: Double?,
        val price: String?
    )
}
```

---

## Phase 3: Docker/Ollama Setup

### 3.1 Docker Compose Updates

**docker-compose.local.yml** additions:
```yaml
services:
  # ... existing services ...

  # Ollama LLM Server
  ollama-local:
    image: ollama/ollama:latest
    container_name: dokus-ollama-local
    ports:
      - "11434:11434"
    volumes:
      - ollama-models-local:/root/.ollama
    networks:
      - dokus-local-network
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
    # For CPU-only (remove deploy section above):
    # environment:
    #   - OLLAMA_NUM_PARALLEL=2
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # Model initialization container (runs once to pull models)
  ollama-init:
    image: ollama/ollama:latest
    container_name: dokus-ollama-init
    depends_on:
      ollama-local:
        condition: service_healthy
    volumes:
      - ollama-models-local:/root/.ollama
    entrypoint: ["/bin/sh", "-c"]
    command:
      - |
        echo "Pulling Mistral 7B..."
        ollama pull mistral:7b
        echo "Pulling Llama 3.1 8B..."
        ollama pull llama3.1:8b
        echo "Models ready!"
    networks:
      - dokus-local-network
    profiles:
      - init

volumes:
  # ... existing volumes ...
  ollama-models-local:
```

**deployment/docker-compose.yml** (production):
```yaml
services:
  # ... existing services ...

  # Ollama for self-hosted production
  ollama:
    image: ollama/ollama:latest
    container_name: dokus-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-models:/root/.ollama
    networks:
      - dokus-network
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
    environment:
      - OLLAMA_NUM_PARALLEL=${OLLAMA_NUM_PARALLEL:-4}
      - OLLAMA_MAX_LOADED_MODELS=${OLLAMA_MAX_LOADED_MODELS:-2}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

volumes:
  ollama-models:
```

### 3.2 Environment Variables

**.env.example** additions:
```bash
# ===========================================
# AI Configuration
# ===========================================

# Default AI provider: "ollama" or "openai"
AI_DEFAULT_PROVIDER=ollama

# Ollama Configuration (for local/self-hosted)
AI_OLLAMA_ENABLED=true
AI_OLLAMA_URL=http://ollama:11434
AI_OLLAMA_MODEL=mistral:7b

# OpenAI Configuration (for cloud)
AI_OPENAI_ENABLED=false
OPENAI_API_KEY=
AI_OPENAI_MODEL=gpt-4o-mini

# Ollama Performance Tuning
OLLAMA_NUM_PARALLEL=4
OLLAMA_MAX_LOADED_MODELS=2
```

### 3.3 dev.sh Updates

Add Ollama management to dev.sh:
```bash
# Add to service startup
start_ollama() {
    print_status loading "Starting Ollama LLM server..."
    docker-compose -f $COMPOSE_FILE up -d ollama-local

    # Wait for Ollama to be ready
    printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Ollama LLM"
    for i in {1..60}; do
        if curl -sf http://localhost:11434/api/tags > /dev/null 2>&1; then
            echo -e "${SOFT_GREEN}◆ Ready${NC}"
            break
        fi
        if [ $i -eq 60 ]; then
            echo -e "${SOFT_RED}◇ Timeout${NC}"
        fi
        echo -n "."
        sleep 2
    done
}

# Add to menu
pull_models() {
    print_gradient_header "🤖 Pull AI Models"
    echo ""
    print_status loading "Pulling Mistral 7B..."
    docker exec dokus-ollama-local ollama pull mistral:7b
    print_status loading "Pulling Llama 3.1 8B..."
    docker exec dokus-ollama-local ollama pull llama3.1:8b
    print_status success "Models ready!"
}
```

### 3.4 Service Configuration Updates

Update each service's **application.conf**:
```hocon
# AI Configuration
ai {
    default-provider = "ollama"
    default-provider = ${?AI_DEFAULT_PROVIDER}

    ollama {
        enabled = true
        enabled = ${?AI_OLLAMA_ENABLED}
        base-url = "http://localhost:11434"
        base-url = ${?AI_OLLAMA_URL}
        default-model = "mistral:7b"
        default-model = ${?AI_OLLAMA_MODEL}
    }

    openai {
        enabled = false
        enabled = ${?AI_OPENAI_ENABLED}
        api-key = ""
        api-key = ${?OPENAI_API_KEY}
        default-model = "gpt-4o-mini"
        default-model = ${?AI_OPENAI_MODEL}
    }
}
```

---

## Implementation Order

### Phase 2 Tasks (Koog Integration)
1. Add Koog dependencies to version catalog
2. Create `foundation/ai` module with config classes
3. Implement AIProviderFactory
4. Create DocumentExtractionAgent
5. Create CategorySuggestionAgent
6. Create SmartSuggestionAgent
7. Implement AIService high-level API
8. Add AI config to AppBaseConfig
9. Integrate with processor service
10. Add unit tests for agents

### Phase 3 Tasks (Docker/Ollama)
1. Update docker-compose.local.yml with Ollama
2. Update deployment/docker-compose.yml
3. Add environment variables to .env.example
4. Update dev.sh with Ollama commands
5. Add service configuration for AI
6. Test local development with Ollama
7. Test OpenAI fallback
8. Document model requirements and GPU setup

---

## Model Recommendations

| Use Case | Ollama Model | OpenAI Model | Notes |
|----------|--------------|--------------|-------|
| Document Extraction | mistral:7b | gpt-4o-mini | Mistral excellent for structured extraction |
| Categorization | llama3.1:8b | gpt-4o-mini | Llama good for classification |
| Smart Suggestions | mistral:7b | gpt-4o-mini | Fast inference needed |

---

## Testing Strategy

1. **Unit Tests**: Mock AI responses for deterministic testing
2. **Integration Tests**: Test against local Ollama with small model
3. **E2E Tests**: Test full document processing pipeline
4. **Benchmark Tests**: Compare Ollama vs OpenAI accuracy/speed

---

## Future Enhancements (Post-MVP)

- Per-tenant AI provider configuration (subscription tiers)
- Fine-tuned models for Belgian invoices
- RAG for company-specific extraction rules
- Batch processing optimization
- Model performance monitoring (Langfuse integration)
