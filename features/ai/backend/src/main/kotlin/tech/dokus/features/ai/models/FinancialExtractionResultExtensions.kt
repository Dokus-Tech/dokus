package tech.dokus.features.ai.models

fun FinancialExtractionResult.confidenceScore(): Double = when (this) {
    is FinancialExtractionResult.Invoice -> data.confidence
    is FinancialExtractionResult.Bill -> data.confidence
    is FinancialExtractionResult.CreditNote -> data.confidence
    is FinancialExtractionResult.Quote -> data.confidence
    is FinancialExtractionResult.ProForma -> data.confidence
    is FinancialExtractionResult.PurchaseOrder -> data.confidence
    is FinancialExtractionResult.Receipt -> data.confidence
    is FinancialExtractionResult.Unsupported -> 0.0
}
