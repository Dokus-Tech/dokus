# Processor Service

Background document processing worker for automated AI-powered document extraction and classification.

## Overview

The Processor service is a dedicated background worker that processes uploaded documents asynchronously:

- **Document Processing**: Polls for pending documents and processes them with AI extraction
- **Multi-Provider Support**: Configurable AI providers (OpenAI, Anthropic, local Ollama)
- **Automatic Retry**: Implements retry logic with configurable attempts and provider fallback
- **Graceful Shutdown**: Proper lifecycle management for clean application shutdown
- **Health Monitoring**: Kubernetes-ready health check endpoints

This service runs independently from the main API, enabling scalable document processing without blocking user requests.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Processor Service                              │
│                     (Ktor + Netty on port 8004)                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│  Health Routes  │   │  Document       │   │   Database      │
│  (/health/*)    │   │  Processing     │   │   Connection    │
│                 │   │  Worker         │   │   (PostgreSQL)  │
└─────────────────┘   └────────┬────────┘   └─────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
    │  Processing  │  │  Document    │  │  Extraction  │
    │  Repository  │  │  Storage     │  │  Provider    │
    │  (DB Polling)│  │  (MinIO/S3)  │  │  Factory     │
    └──────────────┘  └──────────────┘  └──────┬───────┘
                                               │
                               ┌───────────────┴───────────────┐
                               ▼                               ▼
                       ┌───────────────┐               ┌───────────────┐
                       │    OpenAI     │               │  Anthropic    │
                       │   GPT-4o      │               │   Claude      │
                       │  (Vision)     │               │  (Planned)    │
                       └───────────────┘               └───────────────┘
```

### Processing Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Document   │───▶│   Download   │───▶│     AI       │───▶│    Update    │
│   Pending    │    │    from      │    │  Extraction  │    │   Database   │
│  (DB Poll)   │    │   Storage    │    │  (OpenAI)    │    │   Results    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
       │                                       │
       │                                       │
       │                              ┌────────▼────────┐
       │                              │   On Failure:   │
       │                              │  Retry/Fallback │
       │                              └─────────────────┘
       │
┌──────▼──────┐
│  Wait for   │
│  Next Poll  │
│  (5s default)│
└─────────────┘
```

## Key Components

### DocumentProcessingWorker

The main worker class that polls for pending documents and orchestrates processing:

| Feature | Description |
|---------|-------------|
| **Polling-based** | Configurable interval (default 5 seconds) |
| **Batch Processing** | Processes multiple documents per poll cycle |
| **Automatic Retry** | Configurable max attempts (default 3) |
| **Provider Fallback** | Tries alternative providers if primary fails |
| **Graceful Shutdown** | Proper coroutine cancellation and cleanup |

```kotlin
// Worker lifecycle
worker.start()  // Begin polling loop
worker.stop()   // Graceful shutdown with active job cancellation
```

### Extraction Providers

The `AIExtractionProvider` interface defines the contract for document extraction:

| Provider | Status | Model | Capabilities |
|----------|--------|-------|--------------|
| **OpenAIExtractionProvider** | Implemented | GPT-4o | Vision + OCR text analysis |
| **AnthropicExtractionProvider** | Planned | Claude | Vision + text analysis |
| **LocalExtractionProvider** | Planned | Llama 3.2 | Self-hosted via Ollama |

#### OpenAI Provider Features

- **PDF Text Extraction**: Uses Apache PDFBox for text extraction
- **Vision Analysis**: Sends documents as base64-encoded images
- **Structured Output**: Returns typed `ExtractionResult` with confidence scores
- **Document Types**: Invoice, Bill, Expense, Unknown

#### Extraction Result Structure

```kotlin
data class ExtractionResult(
    val documentType: DocumentType,      // Invoice, Bill, Expense, Unknown
    val extractedData: ExtractedDocumentData,
    val rawText: String,
    val confidence: Double,              // 0.0 - 1.0
    val processingTimeMs: Long
)
```

### ExtractionProviderFactory

Manages provider selection and fallback logic:

```kotlin
// Get first available provider (prefers default)
val provider = providerFactory.getFirstAvailableProvider()

// Get all available providers for fallback
val providers = providerFactory.getAvailableProviders()
```

## Health Endpoints

| Endpoint | Purpose | Response |
|----------|---------|----------|
| `GET /health` | General health check | `{"status": "healthy"}` |
| `GET /health/ready` | Readiness probe (K8s) | `{"status": "ready"}` |
| `GET /health/live` | Liveness probe (K8s) | `{"status": "alive"}` |

## Key Files

```
features/processor/backend/
├── src/main/kotlin/ai/dokus/processor/backend/
│   ├── Application.kt                    # Main entry point, server setup
│   ├── config/
│   │   └── DependencyInjection.kt        # Koin DI configuration
│   ├── extraction/
│   │   ├── AIExtractionProvider.kt       # Provider interface & ExtractionResult
│   │   ├── ExtractionProviderFactory.kt  # Provider factory & AIConfig
│   │   └── OpenAIExtractionProvider.kt   # OpenAI GPT-4o implementation
│   ├── plugins/
│   │   └── Database.kt                   # HikariCP PostgreSQL setup
│   └── worker/
│       └── DocumentProcessingWorker.kt   # Main processing loop
├── src/main/resources/
│   ├── application.conf                  # Base HOCON configuration
│   └── application-local.conf            # Local development overrides
└── build.gradle.kts
```

## Configuration

### HOCON Configuration

```hocon
processor {
    # Polling interval in milliseconds
    pollingInterval = 5000
    pollingInterval = ${?PROCESSOR_POLLING_INTERVAL}

    # Maximum retry attempts per document
    maxAttempts = 3
    maxAttempts = ${?PROCESSOR_MAX_ATTEMPTS}

    # Documents to process per poll cycle
    batchSize = 10
    batchSize = ${?PROCESSOR_BATCH_SIZE}

    ai {
        # Default provider: "openai", "anthropic", or "local"
        defaultProvider = "openai"
        defaultProvider = ${?AI_DEFAULT_PROVIDER}

        openai {
            apiKey = ${?OPENAI_API_KEY}
            model = "gpt-4o"
            model = ${?OPENAI_MODEL}
            baseUrl = "https://api.openai.com/v1"
        }

        anthropic {
            apiKey = ${?ANTHROPIC_API_KEY}
            model = "claude-sonnet-4-20250514"
            baseUrl = "https://api.anthropic.com"
        }

        local {
            baseUrl = "http://localhost:11434"
            model = "llama3.2"
        }
    }
}
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8004` |
| `PROCESSOR_POLLING_INTERVAL` | Polling interval (ms) | `5000` |
| `PROCESSOR_MAX_ATTEMPTS` | Max retry attempts | `3` |
| `PROCESSOR_BATCH_SIZE` | Batch size per poll | `10` |
| `AI_DEFAULT_PROVIDER` | Default AI provider | `openai` |
| `OPENAI_API_KEY` | OpenAI API key | Required for OpenAI |
| `OPENAI_MODEL` | OpenAI model | `gpt-4o` |
| `ANTHROPIC_API_KEY` | Anthropic API key | Required for Anthropic |
| `MINIO_PASSWORD` | MinIO storage password | Required |
| `DB_URL` | PostgreSQL connection URL | Required |

### Worker Configuration

```kotlin
data class WorkerConfig(
    val pollingInterval: Long = 5000L,   // 5 seconds
    val batchSize: Int = 10,              // Documents per batch
    val maxAttempts: Int = 3              // Retry attempts
)
```

## Document Types Supported

| Type | Description | Extracted Fields |
|------|-------------|------------------|
| **Invoice** | Outgoing invoices sent to clients | Client info, invoice number, line items, totals, VAT |
| **Bill** | Incoming invoices from suppliers | Supplier info, amounts, VAT, category |
| **Expense** | Receipts and expense tickets | Merchant, amount, VAT, category, payment method |
| **Unknown** | Unrecognized document type | Raw text only |

### Extracted Data Structure

```kotlin
data class ExtractedDocumentData(
    val rawText: String,
    val invoice: ExtractedInvoiceFields?,
    val bill: ExtractedBillFields?,
    val expense: ExtractedExpenseFields?,
    val fieldConfidences: Map<String, Double>
)
```

## Database Integration

The processor uses the shared `ProcessorDocumentProcessingRepository` from the foundation module:

| Operation | Description |
|-----------|-------------|
| `findPendingForProcessing()` | Get documents ready for processing |
| `markAsProcessing()` | Lock document with provider name |
| `markAsProcessed()` | Store extraction results |
| `markAsFailed()` | Record failure with error message |

### Processing States

```
PENDING → PROCESSING → PROCESSED
                    ↘ FAILED (retry if attempts < max)
```

## Storage Integration

Documents are stored in MinIO/S3-compatible object storage:

```kotlin
// Download document from storage
val documentBytes = documentStorage.downloadDocument(storageKey)
```

## Multi-Tenant Isolation

- Documents are isolated by tenant through the processing repository
- Each document processing record includes `tenant_id`
- All database queries filter by tenant context

## Dependencies

- **Ktor** - HTTP server framework with Netty engine
- **Exposed** - Kotlin SQL framework for database operations
- **Koin** - Dependency injection
- **HikariCP** - Database connection pooling
- **Ktor HTTP Client** - For AI API calls
- **Apache PDFBox** - PDF text extraction
- **kotlinx.serialization** - JSON parsing

## Development

### Running Locally

1. Ensure PostgreSQL is running with the required database
2. Ensure MinIO is running for document storage
3. Set required environment variables:
   ```bash
   export OPENAI_API_KEY=your-api-key
   export MINIO_PASSWORD=your-minio-password
   ```
4. Run the application:
   ```bash
   ./gradlew :features:processor:backend:run
   ```

### Testing with Different Providers

```bash
# Use OpenAI (default)
export AI_DEFAULT_PROVIDER=openai
export OPENAI_API_KEY=sk-...

# Use local Ollama
export AI_DEFAULT_PROVIDER=local
# Ensure Ollama is running with llama3.2 model
```

### Health Check

```bash
curl http://localhost:8004/health
# {"status":"healthy"}
```

## Error Handling

| Error Type | Behavior |
|------------|----------|
| Provider unavailable | Try fallback providers |
| Extraction failed (retryable) | Retry on next poll |
| Extraction failed (permanent) | Mark as failed, no retry |
| Storage download failed | Retry (transient error) |
| API timeout | Retry with 2-minute timeout |

## Integration Points

| Service | Integration |
|---------|-------------|
| **Cashflow** | Processes documents uploaded via Cashflow API |
| **Foundation** | Uses shared repositories and storage services |
| **MinIO** | Document storage and retrieval |
| **PostgreSQL** | Processing queue and results storage |
