# AI Service

AI-powered document processing service for automatic document classification, data extraction, and expense categorization.

## Overview

The AI service provides intelligent document processing capabilities for the Dokus platform:

- **Document Classification**: Automatically identifies document types (invoice, receipt, bill)
- **Invoice Extraction**: Extracts structured data from business invoices
- **Receipt Extraction**: Extracts data from retail receipts and purchase proofs
- **Category Suggestions**: Auto-categorizes expenses for Belgian IT freelancers

The service uses a two-step processing pipeline: first classifying the document type, then routing to the appropriate extraction agent.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         AIService                               │
│  (Orchestrates document processing and provides high-level API) │
└─────────────────────────────┬───────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ Classification│   │    Extraction   │   │   Categorization│
│    Agent      │   │     Agents      │   │      Agent      │
└───────────────┘   └─────────────────┘   └─────────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────────────────────────────────────────────────────┐
│                     AIProviderFactory                          │
│        (Creates executors and models based on config)          │
└───────────────────────────────┬───────────────────────────────┘
                                │
                ┌───────────────┴───────────────┐
                ▼                               ▼
        ┌───────────────┐               ┌───────────────┐
        │    Ollama     │               │    OpenAI     │
        │ (Local/Self)  │               │   (Cloud)     │
        └───────────────┘               └───────────────┘
```

### Agents

| Agent | Purpose | Model Purpose |
|-------|---------|---------------|
| `DocumentClassificationAgent` | Classifies documents as INVOICE, RECEIPT, BILL, or UNKNOWN | CLASSIFICATION |
| `InvoiceExtractionAgent` | Extracts vendor, line items, totals, payment info from invoices | DOCUMENT_EXTRACTION |
| `ReceiptExtractionAgent` | Extracts merchant, items, totals, payment method from receipts | DOCUMENT_EXTRACTION |
| `CategorySuggestionAgent` | Suggests expense categories with Belgian tax context | CATEGORIZATION |

### Processing Pipeline

1. **Classification Step**: `DocumentClassificationAgent` analyzes OCR text and determines document type
2. **Extraction Step**: Based on classification, routes to appropriate extraction agent:
   - INVOICE/BILL → `InvoiceExtractionAgent`
   - RECEIPT → `ReceiptExtractionAgent`
3. **Result**: Returns `DocumentProcessingResult` with classification and extracted data

## Key Files

```
features/ai/backend/
├── src/main/kotlin/ai/dokus/ai/
│   ├── agents/
│   │   ├── CategorySuggestionAgent.kt    # Expense categorization
│   │   ├── DocumentClassificationAgent.kt # Document type classification
│   │   ├── InvoiceExtractionAgent.kt     # Invoice data extraction
│   │   └── ReceiptExtractionAgent.kt     # Receipt data extraction
│   ├── config/
│   │   ├── AIConfig.kt                   # Configuration data classes
│   │   ├── AIModule.kt                   # Koin dependency injection
│   │   └── AIProviderFactory.kt          # Provider/model factory
│   ├── models/
│   │   ├── CategorySuggestion.kt         # Category suggestion result
│   │   ├── DocumentClassification.kt     # Classification result
│   │   ├── DocumentProcessingResult.kt   # Combined processing result
│   │   ├── ExtractedInvoiceData.kt       # Invoice extraction model
│   │   └── ExtractedReceiptData.kt       # Receipt extraction model
│   └── service/
│       └── AIService.kt                  # Main service facade
└── build.gradle.kts
```

## Configuration

### Providers

The service supports two AI providers:

| Provider | Use Case | Configuration |
|----------|----------|---------------|
| **Ollama** | Local/self-hosted inference | Base URL, model name |
| **OpenAI** | Cloud-based inference | API key, model name |

### HOCON Configuration

```hocon
ai {
  default-provider = "ollama"  # or "openai"

  ollama {
    enabled = true
    base-url = "http://localhost:11434"
    default-model = "mistral:7b"
  }

  openai {
    enabled = false
    api-key = ${?OPENAI_API_KEY}
    default-model = "gpt-4o-mini"
  }

  models {
    classification = "mistral:7b"
    document-extraction = "mistral:7b"
    categorization = "llama3.1:8b"
    suggestions = "mistral:7b"
  }
}
```

### Model Purposes

Different models can be configured for different tasks:

| Purpose | Description | Default (Ollama) |
|---------|-------------|------------------|
| `classification` | Document type detection | mistral:7b |
| `document-extraction` | Invoice/receipt data extraction | mistral:7b |
| `categorization` | Expense category suggestions | llama3.1:8b |
| `suggestions` | General suggestions | mistral:7b |

## Usage

### Dependency Injection Setup

```kotlin
install(Koin) {
    modules(
        aiModule(),
        // ... other modules
    )
}
```

### Using AIService

```kotlin
val aiService: AIService by inject()

// Two-step document processing (classify then extract)
val result = aiService.processDocument(ocrText)
result.onSuccess { processingResult ->
    val classification = processingResult.classification
    when (val data = processingResult.extractedData) {
        is ExtractedDocumentData.Invoice -> handleInvoice(data.data)
        is ExtractedDocumentData.Receipt -> handleReceipt(data.data)
        is ExtractedDocumentData.Bill -> handleBill(data.data)
    }
}

// Direct extraction (when type is known)
val invoiceData = aiService.extractInvoice(ocrText)
val receiptData = aiService.extractReceipt(ocrText)

// Category suggestion
val suggestion = aiService.suggestCategory(
    description = "MacBook Pro M3",
    merchantName = "Apple Store"
)
// Returns: CategorySuggestion(suggestedCategory="HARDWARE", confidence=0.95, ...)
```

### Classification Only

```kotlin
val classification = aiService.classifyDocument(ocrText)
classification.onSuccess { result ->
    println("Type: ${result.documentType}, Confidence: ${result.confidence}")
}
```

## Document Types

| Type | Description | Extraction Agent |
|------|-------------|------------------|
| `INVOICE` | Formal payment request with invoice number, line items, VAT | `InvoiceExtractionAgent` |
| `RECEIPT` | Proof of purchase from retail, simpler format | `ReceiptExtractionAgent` |
| `BILL` | Utility/service bills (electricity, phone, subscriptions) | `InvoiceExtractionAgent` |
| `UNKNOWN` | Cannot determine type | N/A |

## Expense Categories

The `CategorySuggestionAgent` supports Belgian tax-relevant categories:

| Category | Description |
|----------|-------------|
| `OFFICE_SUPPLIES` | Office equipment, stationery, desk accessories |
| `HARDWARE` | Computers, monitors, peripherals (>500 EUR may need depreciation) |
| `SOFTWARE` | Software licenses, SaaS subscriptions, cloud services |
| `TRAVEL` | Business travel, accommodation, flights, trains |
| `TRANSPORTATION` | Local transport, fuel, parking, car expenses |
| `MEALS` | Business meals, client entertainment (typically 69% deductible) |
| `PROFESSIONAL_SERVICES` | Legal, accounting, consulting fees |
| `UTILITIES` | Internet, phone, electricity (home office portion) |
| `TRAINING` | Courses, conferences, certifications, books (fully deductible) |
| `MARKETING` | Advertising, website hosting, domain names |
| `INSURANCE` | Professional liability, health insurance |
| `RENT` | Office space, coworking memberships |
| `OTHER` | Miscellaneous business expenses |

## Dependencies

- **JetBrains Koog** - AI agent framework for LLM interactions
- **Koin** - Dependency injection
- **Kotlin Coroutines** - Async processing
- **Kotlin Serialization** - JSON parsing

## Development

### Local Development with Ollama

1. Install Ollama: https://ollama.ai
2. Pull required models:
   ```bash
   ollama pull mistral:7b
   ollama pull llama3.1:8b
   ```
3. Start Ollama server (runs on port 11434 by default)
4. Use default configuration (no changes needed)

### Using OpenAI

1. Set environment variable: `export OPENAI_API_KEY=your-key`
2. Configure `default-provider = "openai"` in application.conf
