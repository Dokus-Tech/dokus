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
import tech.dokus.aura.resources.inspector_conflict_confirmation
import tech.dokus.aura.resources.inspector_fuzzy_match
import tech.dokus.aura.resources.inspector_section_sources
import tech.dokus.aura.resources.review_possible_match
import tech.dokus.aura.resources.review_source_corrective_suffix
import tech.dokus.aura.resources.review_source_file_fallback
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun SourcesCard(
    state: DocumentReviewState,
    onResolveSame: () -> Unit,
    onResolveDifferent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sources = state.documentRecord?.sources.orEmpty()
    val pendingReview = state.documentRecord?.pendingMatchReview
    if (sources.size <= 1 && pendingReview == null) return

    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.inspector_section_sources))

        sources.forEach { source ->
            val sourceTitle = source.sourceChannel.localized
            val fallbackName = stringResource(Res.string.review_source_file_fallback)
            val correctiveSuffix = stringResource(Res.string.review_source_corrective_suffix)
            val sourceValue = buildString {
                append(source.filename ?: fallbackName)
                append(" \u00B7 ")
                append(source.arrivalAt.toString())
                if (source.isCorrective) {
                    append(correctiveSuffix)
                }
            }
            FactField(
                label = sourceTitle,
                value = sourceValue
            )
        }

        pendingReview?.let { review ->
            FactField(
                label = stringResource(Res.string.review_possible_match),
                value = when (review.reasonType) {
                    ReviewReason.MaterialConflict ->
                        stringResource(Res.string.inspector_conflict_confirmation)

                    ReviewReason.FuzzyCandidate ->
                        stringResource(Res.string.inspector_fuzzy_match)
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
