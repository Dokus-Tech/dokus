package tech.dokus.ai.agents

import tech.dokus.ai.models.ClassifiedDocumentType
import tech.dokus.ai.models.DocumentClassification
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for classifying document types.
 * Step 1 in the two-step document processing pipeline.
 */
class DocumentClassificationAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true }

    private val systemPrompt = """
        You are a document classification specialist.
        Analyze the provided text and determine the document type.

        Document types:
        - INVOICE: A formal request for payment from a supplier/vendor with invoice number, line items, VAT
        - RECEIPT: A proof of payment/purchase from a store, usually simpler format without detailed line items
        - BILL: A utility or service bill (electricity, phone, internet, subscription services)
        - UNKNOWN: Cannot determine the type

        Key differences:
        - INVOICE: Has invoice number, payment terms, due date, detailed line items, often B2B
        - RECEIPT: Proof of completed payment, often from retail stores, has receipt number, immediate payment
        - BILL: Recurring service charges, account numbers, service periods, utility providers

        Respond with a JSON object containing:
        {
            "documentType": "INVOICE" | "RECEIPT" | "BILL" | "UNKNOWN",
            "confidence": 0.0 to 1.0,
            "reasoning": "Brief explanation of why this classification was chosen"
        }
    """.trimIndent()

    /**
     * Classify the document type from OCR text.
     */
    suspend fun classify(ocrText: String): DocumentClassification {
        logger.debug("Classifying document (${ocrText.length} chars)")

        val userPrompt = """
            Classify this document:

            $ocrText
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "document-classifier",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            parseClassificationResponse(response)
        } catch (e: Exception) {
            logger.error("Failed to classify document", e)
            DocumentClassification(
                documentType = ClassifiedDocumentType.UNKNOWN,
                confidence = 0.0,
                reasoning = "Classification failed: ${e.message}"
            )
        }
    }

    private fun parseClassificationResponse(response: String): DocumentClassification {
        return try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            val jsonString = extractJson(response)
            json.decodeFromString<DocumentClassification>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse classification response: $response", e)
            // Fallback: try to extract document type from text
            fallbackParse(response)
        }
    }

    private fun extractJson(response: String): String {
        // Remove markdown code blocks if present
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // Find JSON object in the response
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }

    private fun fallbackParse(response: String): DocumentClassification {
        val upperResponse = response.uppercase()
        val documentType = when {
            "INVOICE" in upperResponse -> ClassifiedDocumentType.INVOICE
            "RECEIPT" in upperResponse -> ClassifiedDocumentType.RECEIPT
            "BILL" in upperResponse -> ClassifiedDocumentType.BILL
            else -> ClassifiedDocumentType.UNKNOWN
        }

        return DocumentClassification(
            documentType = documentType,
            confidence = 0.5, // Lower confidence for fallback
            reasoning = "Fallback classification based on keyword detection"
        )
    }
}
