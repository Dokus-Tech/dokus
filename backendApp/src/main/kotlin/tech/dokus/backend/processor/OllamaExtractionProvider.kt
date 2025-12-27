package tech.dokus.backend.processor

import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.model.ExtractedDocumentData
import tech.dokus.foundation.ktor.utils.loggerFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import kotlin.system.measureTimeMillis

/**
 * Ollama-based document extraction provider.
 * Uses local Ollama for text-based document understanding.
 *
 * Note: Unlike OpenAI GPT-4o, Ollama models typically don't have vision capabilities,
 * so this provider relies on PDF text extraction.
 */
class OllamaExtractionProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "mistral:7b"
) : AIExtractionProvider {

    override val name = "ollama"

    private val logger = loggerFor()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            // Check if Ollama is reachable
            val response = httpClient.post("$baseUrl/api/tags")
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.debug("Ollama not available at $baseUrl: ${e.message}")
            false
        }
    }

    override suspend fun extract(
        documentBytes: ByteArray,
        contentType: String,
        filename: String
    ): ExtractionResult {
        var rawText = ""
        var result: ExtractionResult? = null
        val processingTime = measureTimeMillis {
            try {
                // Extract text from document
                rawText = extractText(documentBytes, contentType)

                if (rawText.isBlank()) {
                    logger.warn("No text could be extracted from document $filename")
                    // Return Unknown with empty extraction for non-text documents
                    result = ExtractionResult(
                        documentType = DocumentType.Unknown,
                        extractedData = ExtractedDocumentData(rawText = ""),
                        rawText = "",
                        confidence = 0.0,
                        processingTimeMs = 0
                    )
                    return@measureTimeMillis
                }

                // Call Ollama to analyze the document
                val response = callOllama(rawText, filename)

                // Parse the response
                result = parseResponse(response, rawText)
            } catch (e: ExtractionException) {
                throw e
            } catch (e: Exception) {
                logger.error("Ollama extraction failed", e)
                throw ExtractionException(
                    "Failed to extract document: ${e.message}",
                    name,
                    isRetryable = true,
                    cause = e
                )
            }
        }

        return result!!.copy(processingTimeMs = processingTime)
    }

    private fun extractText(documentBytes: ByteArray, contentType: String): String {
        return when {
            contentType.contains("pdf") -> extractPdfText(documentBytes)
            else -> ""
        }
    }

    private fun extractPdfText(documentBytes: ByteArray): String {
        return try {
            Loader.loadPDF(documentBytes).use { document ->
                PDFTextStripper().getText(document)
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract PDF text", e)
            ""
        }
    }

    private suspend fun callOllama(extractedText: String, filename: String): OllamaResponse {
        val request = OllamaRequest(
            model = model,
            messages = listOf(
                OllamaMessage(
                    role = "system",
                    content = SYSTEM_PROMPT
                ),
                OllamaMessage(
                    role = "user",
                    content = """
                        |Filename: $filename
                        |
                        |Document text:
                        |$extractedText
                        |
                        |Extract the financial data from this document and return a valid JSON response.
                    """.trimMargin()
                )
            ),
            stream = false,
            format = "json"
        )

        logger.debug("Calling Ollama at $baseUrl with model $model")

        val response = try {
            httpClient.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(OllamaRequest.serializer(), request))
            }
        } catch (e: Exception) {
            logger.error("Failed to connect to Ollama at $baseUrl", e)
            throw ExtractionException(
                "Failed to connect to Ollama: ${e.message}",
                name,
                isRetryable = true,
                cause = e
            )
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            logger.error("Ollama API error: ${response.status} - $errorBody")
            throw ExtractionException(
                "Ollama API returned ${response.status}",
                name,
                isRetryable = response.status.value >= 500
            )
        }

        return response.body()
    }

    private fun parseResponse(response: OllamaResponse, rawText: String): ExtractionResult {
        val content = response.message?.content
            ?: throw ExtractionException("Empty response from Ollama", name, isRetryable = true)

        logger.debug("Ollama response: $content")

        val parsed = try {
            json.decodeFromString<ExtractedJsonResponse>(content)
        } catch (e: Exception) {
            logger.error("Failed to parse Ollama response: $content", e)
            throw ExtractionException("Failed to parse extraction response", name, isRetryable = false, cause = e)
        }

        val documentType = try {
            DocumentType.valueOf(parsed.documentType)
        } catch (e: Exception) {
            DocumentType.Unknown
        }

        val extractedData = ExtractedDocumentData(
            rawText = rawText,
            invoice = parsed.invoice?.toExtractedFields(),
            bill = parsed.bill?.toExtractedFields(),
            expense = parsed.expense?.toExtractedFields(),
            fieldConfidences = parsed.fieldConfidences ?: emptyMap()
        )

        return ExtractionResult(
            documentType = documentType,
            extractedData = extractedData,
            rawText = rawText,
            confidence = parsed.confidence,
            processingTimeMs = 0
        )
    }

    companion object {
        private val SYSTEM_PROMPT = """
You are a document extraction AI specialized in financial documents. Extract structured data from invoices, bills, expenses, and receipts.

Analyze the document and return a JSON object with the following structure:

{
    "documentType": "Invoice" | "Bill" | "Expense" | "Unknown",
    "confidence": 0.0-1.0,
    "fieldConfidences": { "fieldName": 0.0-1.0, ... },
    "invoice": { ... } | null,
    "bill": { ... } | null,
    "expense": { ... } | null
}

For INVOICE (outgoing invoice you send to clients):
- clientName, clientVatNumber, clientEmail, clientAddress
- invoiceNumber, issueDate (YYYY-MM-DD), dueDate
- items: [{description, quantity, unitPrice, vatRate, lineTotal}]
- subtotalAmount, vatAmount, totalAmount, currency, notes, paymentTerms, bankAccount

For BILL (incoming invoice from suppliers):
- supplierName, supplierVatNumber, supplierAddress
- invoiceNumber, issueDate, dueDate
- amount, vatAmount, vatRate, currency, category, description, notes, paymentTerms, bankAccount

For EXPENSE (receipt/expense ticket):
- merchant, merchantAddress, merchantVatNumber
- date, amount, vatAmount, vatRate, currency
- category, description, isDeductible, deductiblePercentage, paymentMethod, notes, receiptNumber

Categories: OfficeSupplies, Travel, Meals, Software, Hardware, Utilities, Rent, Insurance, Marketing, ProfessionalServices, Telecommunications, Vehicle, Other
Currencies: EUR, USD, GBP (default EUR)
Payment Methods: Cash, Card, BankTransfer, Other

All monetary amounts should be decimal strings. Dates in YYYY-MM-DD format.
VAT rates: "0.00", "6.00", "12.00", "21.00"

IMPORTANT: Return ONLY valid JSON, no additional text.
""".trimIndent()
    }
}

// Ollama API request/response types
@Serializable
private data class OllamaRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean = false,
    val format: String = "json"
)

@Serializable
private data class OllamaMessage(
    val role: String,
    val content: String
)

@Serializable
private data class OllamaResponse(
    val message: OllamaResponseMessage? = null
)

@Serializable
private data class OllamaResponseMessage(
    val role: String? = null,
    val content: String? = null
)
