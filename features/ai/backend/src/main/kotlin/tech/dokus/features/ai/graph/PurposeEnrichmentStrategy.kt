package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.features.ai.models.PurposeEnrichmentInput
import tech.dokus.features.ai.models.PurposeEnrichmentResult

private const val PurposeBaseMaxLength = 72

fun purposeEnrichmentGraph(): AIAgentGraphStrategy<PurposeEnrichmentInput, PurposeEnrichmentResult> {
    return strategy<PurposeEnrichmentInput, PurposeEnrichmentResult>("purpose-enrichment-graph") {
        val resolvePurpose by node<PurposeEnrichmentInput, PurposeEnrichmentResult>("resolve-purpose") { input ->
            val templateBase = input.templatePurposeBase?.sanitizePurposeBase()
            val ragBase = input.ragPurposeBaseCandidates
                .firstNotNullOfOrNull { candidate -> candidate.sanitizePurposeBase() }
            val fallbackBase = input.fallbackPurposeBase.sanitizePurposeBase()
                ?: defaultPurposeBase(input.documentType)

            val resolvedBase = templateBase ?: ragBase ?: fallbackBase
            val resolvedMode = when {
                !templateBase.isNullOrBlank() -> input.templatePeriodMode ?: PurposePeriodMode.IssueMonth
                else -> PurposePeriodMode.IssueMonth
            }
            val source = when {
                !templateBase.isNullOrBlank() -> DocumentPurposeSource.AiTemplate
                !ragBase.isNullOrBlank() -> DocumentPurposeSource.AiRag
                else -> null
            }

            val periodDate = when (resolvedMode) {
                PurposePeriodMode.IssueMonth -> input.issueDate
                PurposePeriodMode.ServicePeriod -> input.servicePeriodStart ?: input.issueDate
                PurposePeriodMode.None -> null
            }
            val (periodYear, periodMonth) = if (periodDate != null) {
                periodDate.year to periodDate.monthNumber
            } else {
                null to null
            }
            val rendered = renderPurpose(
                supplierDisplayName = input.supplierDisplayName,
                purposeBase = resolvedBase,
                periodYear = periodYear,
                periodMonth = periodMonth,
                periodMode = resolvedMode
            )

            PurposeEnrichmentResult(
                purposeBase = resolvedBase,
                purposePeriodYear = periodYear,
                purposePeriodMonth = periodMonth,
                purposeRendered = rendered,
                purposeSource = source,
                purposePeriodMode = resolvedMode
            )
        }

        edge(nodeStart forwardTo resolvePurpose)
        edge(resolvePurpose forwardTo nodeFinish)
    }
}

private fun String?.sanitizePurposeBase(): String? {
    val cleaned = this
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.take(PurposeBaseMaxLength)
    return cleaned?.takeIf { it.isNotBlank() }
}

private fun defaultPurposeBase(documentType: DocumentType): String = when (documentType) {
    DocumentType.Invoice -> "invoice"
    DocumentType.CreditNote -> "credit note"
    DocumentType.Receipt -> "receipt"
    else -> "document"
}

private fun renderPurpose(
    supplierDisplayName: String?,
    purposeBase: String?,
    periodYear: Int?,
    periodMonth: Int?,
    periodMode: PurposePeriodMode
): String? {
    val base = purposeBase?.takeIf { it.isNotBlank() } ?: return null
    val supplier = supplierDisplayName
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }

    val periodLabel = if (periodMode == PurposePeriodMode.None || periodYear == null || periodMonth == null) {
        null
    } else {
        "${monthLabel(periodMonth)} $periodYear"
    }

    return buildString {
        if (!supplier.isNullOrBlank()) {
            append(supplier)
            append(" - ")
        }
        append(base)
        if (!periodLabel.isNullOrBlank()) {
            append(" ")
            append(periodLabel)
        }
    }.takeIf { it.isNotBlank() }
}

private fun monthLabel(monthNumber: Int): String {
    val months = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )
    return months.getOrElse(monthNumber - 1) { "Month" }
}
