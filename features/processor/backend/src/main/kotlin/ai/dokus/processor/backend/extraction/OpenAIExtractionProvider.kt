package ai.dokus.processor.backend.extraction

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.ExtractedBillFields
import ai.dokus.foundation.domain.model.ExtractedDocumentData
import ai.dokus.foundation.domain.model.ExtractedExpenseFields
import ai.dokus.foundation.domain.model.ExtractedInvoiceFields
import ai.dokus.foundation.domain.model.ExtractedLineItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.util.Base64
import kotlin.system.measureTimeMillis

/**
 * OpenAI-based document extraction provider.
 * Uses GPT-4o for vision-based document understanding.
 */
class OpenAIExtractionProvider(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val model: String = "gpt-4o",
    private val baseUrl: String = "https://api.openai.com/v1"
) : AIExtractionProvider {

    override val name = "openai"

    private val logger = LoggerFactory.getLogger(OpenAIExtractionProvider::class.java)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun isAvailable(): Boolean {
        return apiKey.isNotBlank()
    }

    override suspend fun extract(
        documentBytes: ByteArray,
        contentType: String,
        filename: String
    ): ExtractionResult {
        if (!isAvailable()) {
            throw ExtractionException(
                "OpenAI API key not configured",
                name,
                isRetryable = false
            )
        }

        var rawText = ""
        var result: ExtractionResult? = null
        val processingTime = measureTimeMillis {
            try {
                // Extract text from document
                rawText = extractText(documentBytes, contentType)

                // Call OpenAI to analyze the document
                val response = callOpenAI(documentBytes, contentType, rawText, filename)

                // Parse the response
                result = parseResponse(response, rawText)
            } catch (e: ExtractionException) {
                throw e
            } catch (e: Exception) {
                logger.error("OpenAI extraction failed", e)
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
            contentType.startsWith("image/") -> "" // Will rely on vision
            else -> ""
        }
    }

    private fun extractPdfText(documentBytes: ByteArray): String {
        return try {
            Loader.loadPDF(documentBytes).use { document ->
                PDFTextStripper().getText(document)
            }
        } catch (e: Exception) {
            logger.warn("Failed to extract PDF text, will rely on vision", e)
            ""
        }
    }

    private suspend fun callOpenAI(
        documentBytes: ByteArray,
        contentType: String,
        extractedText: String,
        filename: String
    ): OpenAIResponse {
        val messages = buildMessages(documentBytes, contentType, extractedText, filename)

        val requestJson = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("messages", messages)
            put("max_tokens", JsonPrimitive(4096))
            put("temperature", JsonPrimitive(0.1))
            put("response_format", buildJsonObject {
                put("type", JsonPrimitive("json_object"))
            })
        }

        val response = httpClient.post("$baseUrl/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestJson.toString())
        }

        if (response.status.value !in 200..299) {
            val errorBody = response.body<String>()
            logger.error("OpenAI API error: ${response.status} - $errorBody")
            throw ExtractionException(
                "OpenAI API returned ${response.status}",
                name,
                isRetryable = response.status.value >= 500
            )
        }

        return response.body()
    }

    private fun buildMessages(
        documentBytes: ByteArray,
        contentType: String,
        extractedText: String,
        filename: String
    ): kotlinx.serialization.json.JsonArray {
        val systemPrompt = SYSTEM_PROMPT

        val userContent = mutableListOf<kotlinx.serialization.json.JsonElement>()

        // Add extracted text if available
        if (extractedText.isNotBlank()) {
            userContent.add(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive("Extracted text from document:\n\n$extractedText"))
            })
        }

        // Add image/document for vision
        if (contentType.startsWith("image/") || contentType.contains("pdf")) {
            val base64 = Base64.getEncoder().encodeToString(documentBytes)
            val mediaType = if (contentType.contains("pdf")) "image/png" else contentType
            userContent.add(buildJsonObject {
                put("type", JsonPrimitive("image_url"))
                put("image_url", buildJsonObject {
                    put("url", JsonPrimitive("data:$mediaType;base64,$base64"))
                    put("detail", JsonPrimitive("high"))
                })
            })
        }

        userContent.add(buildJsonObject {
            put("type", JsonPrimitive("text"))
            put("text", JsonPrimitive("Filename: $filename\n\nExtract the financial data from this document."))
        })

        return buildJsonArray {
            add(buildJsonObject {
                put("role", JsonPrimitive("system"))
                put("content", JsonPrimitive(systemPrompt))
            })
            add(buildJsonObject {
                put("role", JsonPrimitive("user"))
                put("content", buildJsonArray {
                    userContent.forEach { add(it) }
                })
            })
        }
    }

    private fun parseResponse(response: OpenAIResponse, rawText: String): ExtractionResult {
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw ExtractionException("Empty response from OpenAI", name, isRetryable = true)

        logger.debug("OpenAI response: $content")

        val parsed = try {
            json.decodeFromString<ExtractedJsonResponse>(content)
        } catch (e: Exception) {
            logger.error("Failed to parse OpenAI response: $content", e)
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
""".trimIndent()
    }
}

// OpenAI API response types
@Serializable
private data class OpenAIResponse(
    val choices: List<Choice>
)

@Serializable
private data class Choice(
    val message: ResponseMessage
)

@Serializable
private data class ResponseMessage(
    val content: String
)

// Extracted response types
@Serializable
private data class ExtractedJsonResponse(
    val documentType: String,
    val confidence: Double,
    val fieldConfidences: Map<String, Double>? = null,
    val invoice: ExtractedInvoiceJson? = null,
    val bill: ExtractedBillJson? = null,
    val expense: ExtractedExpenseJson? = null
)

@Serializable
private data class ExtractedInvoiceJson(
    val clientName: String? = null,
    val clientVatNumber: String? = null,
    val clientEmail: String? = null,
    val clientAddress: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val items: List<ExtractedLineItemJson>? = null,
    val subtotalAmount: String? = null,
    val vatAmount: String? = null,
    val totalAmount: String? = null,
    val currency: String? = null,
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
) {
    fun toExtractedFields() = ExtractedInvoiceFields(
        clientName = clientName,
        clientVatNumber = clientVatNumber,
        clientEmail = clientEmail,
        clientAddress = clientAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.let { parseDate(it) },
        dueDate = dueDate?.let { parseDate(it) },
        items = items?.map { it.toExtractedLineItem() },
        subtotalAmount = subtotalAmount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        totalAmount = totalAmount?.let { Money(it) },
        currency = currency?.let { parseCurrency(it) },
        notes = notes,
        paymentTerms = paymentTerms,
        bankAccount = bankAccount
    )
}

@Serializable
private data class ExtractedLineItemJson(
    val description: String? = null,
    val quantity: Double? = null,
    val unitPrice: String? = null,
    val vatRate: String? = null,
    val lineTotal: String? = null
) {
    fun toExtractedLineItem() = ExtractedLineItem(
        description = description,
        quantity = quantity,
        unitPrice = unitPrice?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        lineTotal = lineTotal?.let { Money(it) }
    )
}

@Serializable
private data class ExtractedBillJson(
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,
    val supplierAddress: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val amount: String? = null,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
) {
    fun toExtractedFields() = ExtractedBillFields(
        supplierName = supplierName,
        supplierVatNumber = supplierVatNumber,
        supplierAddress = supplierAddress,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate?.let { parseDate(it) },
        dueDate = dueDate?.let { parseDate(it) },
        amount = amount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        currency = currency?.let { parseCurrency(it) },
        category = category?.let { parseCategory(it) },
        description = description,
        notes = notes,
        paymentTerms = paymentTerms,
        bankAccount = bankAccount
    )
}

@Serializable
private data class ExtractedExpenseJson(
    val merchant: String? = null,
    val merchantAddress: String? = null,
    val merchantVatNumber: String? = null,
    val date: String? = null,
    val amount: String? = null,
    val vatAmount: String? = null,
    val vatRate: String? = null,
    val currency: String? = null,
    val category: String? = null,
    val description: String? = null,
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Double? = null,
    val paymentMethod: String? = null,
    val notes: String? = null,
    val receiptNumber: String? = null
) {
    fun toExtractedFields() = ExtractedExpenseFields(
        merchant = merchant,
        merchantAddress = merchantAddress,
        merchantVatNumber = merchantVatNumber,
        date = date?.let { parseDate(it) },
        amount = amount?.let { Money(it) },
        vatAmount = vatAmount?.let { Money(it) },
        vatRate = vatRate?.let { VatRate(it) },
        currency = currency?.let { parseCurrency(it) },
        category = category?.let { parseCategory(it) },
        description = description,
        isDeductible = isDeductible,
        deductiblePercentage = deductiblePercentage?.let { Percentage(it.toString()) },
        paymentMethod = paymentMethod?.let { parsePaymentMethod(it) },
        notes = notes,
        receiptNumber = receiptNumber
    )
}

private fun parseDate(dateStr: String): LocalDate? {
    return try {
        LocalDate.parse(dateStr)
    } catch (e: Exception) {
        null
    }
}

private fun parseCategory(categoryStr: String): ExpenseCategory? {
    return try {
        ExpenseCategory.valueOf(categoryStr)
    } catch (e: Exception) {
        ExpenseCategory.Other
    }
}

private fun parsePaymentMethod(methodStr: String): PaymentMethod? {
    return try {
        PaymentMethod.valueOf(methodStr)
    } catch (e: Exception) {
        PaymentMethod.Other
    }
}

private fun parseCurrency(currencyStr: String): Currency? {
    return when (currencyStr.uppercase()) {
        "EUR" -> Currency.Eur
        "USD" -> Currency.Usd
        "GBP" -> Currency.Gbp
        "CHF" -> Currency.Chf
        "CAD" -> Currency.Cad
        "AUD" -> Currency.Aud
        else -> Currency.Eur  // Default to EUR for European context
    }
}
