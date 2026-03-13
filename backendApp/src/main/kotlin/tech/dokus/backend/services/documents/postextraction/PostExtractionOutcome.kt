package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.documents.DocumentIntakeServiceResult

sealed interface PostExtractionOutcome {
    data object UnsupportedConfirmed : PostExtractionOutcome
    data class TruthResolved(val matchOutcome: DocumentIntakeServiceResult) : PostExtractionOutcome
    data object BankStatementProcessed : PostExtractionOutcome
    data object StandardProcessed : PostExtractionOutcome
    data object Skipped : PostExtractionOutcome
}
