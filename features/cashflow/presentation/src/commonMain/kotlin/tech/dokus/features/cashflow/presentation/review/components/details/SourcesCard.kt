package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_match_review_different_document
import tech.dokus.aura.resources.cashflow_match_review_same_document
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

@Composable
internal fun SourcesCard(
    state: DocumentReviewState.Content,
    onResolveSame: () -> Unit,
    onResolveDifferent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sources = state.document.sources
    val pendingReview = state.document.pendingMatchReview
    if (sources.size <= 1 && pendingReview == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = "Sources")

        sources.forEach { source ->
            val sourceTitle = source.sourceChannel.displayLabel()
            val sourceValue = buildString {
                append(source.filename ?: "Source file")
                append(" · ")
                append(source.arrivalAt.toString())
                if (source.isCorrective) {
                    append(" · corrective")
                }
            }
            FactField(
                label = sourceTitle,
                value = sourceValue
            )
        }

        pendingReview?.let { review ->
            FactField(
                label = "Possible match",
                value = when (review.reasonType) {
                    DocumentMatchReviewReasonType.MaterialConflict ->
                        "Conflicting financial facts require confirmation."

                    DocumentMatchReviewReasonType.FuzzyCandidate ->
                        "Potential same document found with fuzzy identity match."
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onResolveSame,
                    enabled = !state.isResolvingMatchReview
                ) {
                    Text(stringResource(Res.string.cashflow_match_review_same_document))
                }
                TextButton(
                    onClick = onResolveDifferent,
                    enabled = !state.isResolvingMatchReview
                ) {
                    Text(stringResource(Res.string.cashflow_match_review_different_document))
                }
            }
        }
    }
}

private fun DocumentSource.displayLabel(): String = when (this) {
    DocumentSource.Peppol -> "PEPPOL"
    DocumentSource.Email -> "Email"
    DocumentSource.Upload -> "Upload"
    DocumentSource.Manual -> "Manual"
}
