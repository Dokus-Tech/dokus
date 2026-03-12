package tech.dokus.features.ai.agents

import kotlinx.serialization.SerializationException
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.ai.config.KoogAgentRunner
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.acceptDocumentGraph
import tech.dokus.features.ai.graph.purposeEnrichmentGraph
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.models.DirectionResolution
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.PurposeEnrichmentInput
import tech.dokus.features.ai.models.PurposeEnrichmentResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

class DocumentProcessingAgent(
    private val agentRunner: KoogAgentRunner,
    private val aiConfig: AIConfig,
    private val documentFetcher: DocumentFetcher,
) {
    private val logger = loggerFor()

    suspend fun process(input: AcceptDocumentInput): DocumentAiProcessingResult {
        return try {
            agentRunner.run(
                input = input,
                strategy = acceptDocumentGraph(aiConfig, documentFetcher),
                agentName = "document-processing",
                systemPrompt = "You classify and extract structured financial data from provided document pages."
            )
        } catch (exception: Exception) {
            if (isPeppolDeserializationFailure(exception) && input.peppolStructuredSnapshotJson != null) {
                logger.warn(
                    "PEPPOL snapshot deserialization failed for documentId=${input.documentId}, falling back to vision",
                    exception
                )
                try {
                    agentRunner.run(
                        input = input.copy(peppolStructuredSnapshotJson = null),
                        strategy = acceptDocumentGraph(aiConfig, documentFetcher),
                        agentName = "document-processing",
                        systemPrompt = "You classify and extract structured financial data from provided document pages."
                    )
                } catch (fallbackException: Exception) {
                    logger.warn("Vision fallback also failed: documentId=${input.documentId}", fallbackException)
                    failureResult(input, fallbackException)
                }
            } else {
                logger.warn("Document processing failed: documentId=${input.documentId}", exception)
                failureResult(input, exception)
            }
        }
    }

    suspend fun enrichPurpose(input: PurposeEnrichmentInput): PurposeEnrichmentResult {
        return agentRunner.run(
            input = input,
            strategy = purposeEnrichmentGraph(),
            agentName = "purpose-enrichment",
            systemPrompt = "You render canonical document purpose labels."
        )
    }

    private fun failureResult(
        input: AcceptDocumentInput,
        exception: Exception,
    ): DocumentAiProcessingResult {
        return DocumentAiProcessingResult(
            classification = ClassificationResult(
                documentType = DocumentType.Unknown,
                confidence = 0.0,
                language = "unknown",
                reasoning = "Processing failed before a valid result was produced."
            ),
            extraction = FinancialExtractionResult.Unsupported(
                documentType = DocumentType.Unknown.name,
                reason = failureReason(exception)
            ),
            directionResolution = DirectionResolution(
                tenantVat = input.tenant.vatNumber.normalized.takeIf { it.isNotBlank() },
                reasoning = "Direction was not resolved because processing failed."
            ),
            auditReport = contractFailureAudit(exception)
        )
    }

    private fun contractFailureAudit(exception: Exception): AuditReport {
        return AuditReport.fromChecks(
            listOf(
                AuditCheck.criticalFailure(
                    type = CheckType.AI_CONTRACT,
                    field = "finishTool",
                    message = "AI processing failed to return a valid native finish tool call.",
                    hint = "Retry the run. Treat assistant text, raw JSON, repeated wrong tools, and invalid finish payloads as hard failures.",
                    expected = "native finish tool call",
                    actual = failureReason(exception)
                )
            )
        )
    }

    private fun failureReason(exception: Exception): String {
        val message = exception.message?.takeIf { it.isNotBlank() } ?: "no error message"
        return "processing failed with ${exception::class.simpleName}: $message"
    }

    private fun isPeppolDeserializationFailure(exception: Exception): Boolean {
        var current: Throwable? = exception
        while (current != null) {
            if (current is SerializationException) return true
            current = current.cause
        }
        return false
    }
}
