package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.comparison_existing_label
import tech.dokus.aura.resources.comparison_incoming_label
import tech.dokus.aura.resources.comparison_title
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun DocumentComparisonPane(
    existingUiData: DocumentUiData,
    incomingUiData: DocumentUiData?,
    existingCounterpartyName: String,
    incomingCounterpartyName: String,
    reasonType: ReviewReason,
    onSameDocument: () -> Unit,
    onDifferentDocument: () -> Unit,
    isResolving: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium,
                ),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.comparison_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ComparisonReasonBanner(reasonType = reasonType)
        }

        HorizontalDivider()

        // Side-by-side cards
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Constraints.Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            // Existing document
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                ComparisonLabel(
                    text = stringResource(Res.string.comparison_existing_label),
                    counterpartyName = existingCounterpartyName,
                )
                ComparisonDocumentCard(
                    uiData = existingUiData,
                    counterpartyName = existingCounterpartyName,
                )
            }

            // Incoming document
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                ComparisonLabel(
                    text = stringResource(Res.string.comparison_incoming_label),
                    counterpartyName = incomingCounterpartyName,
                )
                if (incomingUiData != null) {
                    ComparisonDocumentCard(
                        uiData = incomingUiData,
                        counterpartyName = incomingCounterpartyName,
                    )
                }
            }
        }

        // Action bar
        HorizontalDivider()
        ComparisonActionBar(
            onSameDocument = onSameDocument,
            onDifferentDocument = onDifferentDocument,
            isResolving = isResolving,
        )
    }
}

@Composable
private fun ComparisonLabel(
    text: String,
    counterpartyName: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = counterpartyName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
