package tech.dokus.features.ai.models

data class ResolvedExtraction(
    val extraction: FinancialExtractionResult,
    val directionResolution: DirectionResolution
)
