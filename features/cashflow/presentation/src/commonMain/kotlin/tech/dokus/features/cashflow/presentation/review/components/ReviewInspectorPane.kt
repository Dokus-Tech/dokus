package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_detail_confirmed
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.compressedStatusDetailLocalized
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.features.cashflow.presentation.review.statusBadgeLocalized
import tech.dokus.foundation.aura.components.icons.LockIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

@Composable
internal fun ReviewInspectorPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        InspectorHeader(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.medium,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            InspectorAmountSection(state = state)
            InspectorTimelineSection(state = state)
            InspectorReferenceSection(state = state)
            InspectorContactSection(state = state)
            InspectorSourcesSection(
                state = state,
                onIntent = onIntent,
            )
            InspectorPaymentSection(
                state = state,
                onIntent = onIntent,
            )
        }

        OutlinedButton(
            onClick = { onIntent(DocumentReviewIntent.RequestAmendment) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
        ) {
            Text("Request amendment")
        }
    }
}

@Composable
private fun InspectorHeader(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompressedStatusLine(state)

            if (!state.isDocumentConfirmed && !state.isDocumentRejected) {
                if (state.isEditMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                        OutlinedButton(onClick = { onIntent(DocumentReviewIntent.CancelEditMode) }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onIntent(DocumentReviewIntent.SaveDraft) },
                            enabled = !state.isSaving,
                        ) {
                            Text("Save")
                        }
                    }
                } else {
                    OutlinedButton(onClick = { onIntent(DocumentReviewIntent.EnterEditMode) }) {
                        Text("Edit")
                    }
                }
            }
        }

        if (!state.isDocumentConfirmed && !state.isDocumentRejected &&
            state.financialStatus == ReviewFinancialStatus.Review
        ) {
            Button(
                onClick = { onIntent(DocumentReviewIntent.Confirm) },
                enabled = state.canConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Confirm document")
            }
        } else if (state.canRecordPayment) {
            OutlinedButton(
                onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Record payment")
            }
        }
    }
}

@Composable
private fun CompressedStatusLine(state: DocumentReviewState.Content) {
    val statusColor = state.financialStatus.financialStatusColorized
    val detailText = state.compressedStatusDetailLocalized

    if (state.financialStatus == ReviewFinancialStatus.Review && !state.isDocumentConfirmed) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            StatusDot(type = state.financialStatus.dotType, size = 6.dp)
            Text(
                text = state.statusBadgeLocalized,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
        }
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        LockIcon(modifier = Modifier, tint = MaterialTheme.colorScheme.textMuted)
        Text(
            text = stringResource(Res.string.document_detail_confirmed),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "\u00b7",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
        )
        StatusDot(type = state.financialStatus.dotType, size = 6.dp)
        Text(
            text = detailText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPanePaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Paid),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneUnpaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneOverduePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Overdue),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneReviewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = null, isDocumentConfirmed = false),
            onIntent = {},
        )
    }
}
