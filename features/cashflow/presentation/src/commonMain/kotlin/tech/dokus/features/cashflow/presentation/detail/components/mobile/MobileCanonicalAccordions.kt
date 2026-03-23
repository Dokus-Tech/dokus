package tech.dokus.features.cashflow.presentation.detail.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_match_review_different_document
import tech.dokus.aura.resources.cashflow_match_review_same_document
import tech.dokus.aura.resources.document_sources_independently_verified
import tech.dokus.domain.Money
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.detail.models.LineItemUiData
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.hasCrossMatchedSources
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.extensions.sourceListLabelLocalized
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun MobileItemsAccordion(
    lineItems: List<LineItemUiData>,
    subtotalAmount: Money?,
    vatAmount: Money?,
    totalAmount: Money?,
    currencySign: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    MobileAccordionCard(
        title = "Items",
        count = lineItems.size,
        expanded = expanded,
        onToggle = onToggle,
    ) {
        lineItems.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = item.displayAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = Constraints.Spacing.small),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MobileTotalsBlock(
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            currencySign = currencySign,
        )
    }
}

@Composable
internal fun MobileSourcesAccordion(
    state: DocumentDetailState,
    isAccountantReadOnly: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onIntent: (DocumentDetailIntent) -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
) {
    MobileAccordionCard(
        title = "Sources",
        count = state.documentRecord?.sources.orEmpty().size,
        expanded = expanded,
        onToggle = onToggle,
    ) {
        state.documentRecord?.sources.orEmpty().forEach { source ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSource(source.id) }
                    .padding(vertical = Constraints.Spacing.xSmall),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = source.sourceChannel.localizedUppercase,
                    style = MaterialTheme.typography.labelSmall,
                    color = source.sourceChannel.colorized,
                )
                Text(
                    text = source.sourceChannel.sourceListLabelLocalized,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                PIcon(
                    icon = Lucide.ChevronRight,
                    description = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        state.documentRecord?.pendingMatchReview?.let { pendingReview ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val reasonText = when (pendingReview.reasonType) {
                ReviewReason.MaterialConflict -> {
                    "Conflicting source facts need your confirmation."
                }

                ReviewReason.FuzzyCandidate -> {
                    "Possible duplicate source found."
                }
            }
            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
            if (!isAccountantReadOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                    TextButton(
                        onClick = { onIntent(DocumentDetailIntent.ResolvePossibleMatchSame) },
                        enabled = !state.isResolvingMatchReview,
                    ) {
                        Text(stringResource(Res.string.cashflow_match_review_same_document))
                    }
                    TextButton(
                        onClick = { onIntent(DocumentDetailIntent.ResolvePossibleMatchDifferent) },
                        enabled = !state.isResolvingMatchReview,
                    ) {
                        Text(stringResource(Res.string.cashflow_match_review_different_document))
                    }
                }
            }
        }

        if (state.hasCrossMatchedSources) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.greenSoft.copy(alpha = 0.35f),
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.small,
                        vertical = Constraints.Spacing.xSmall,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    StatusDot(type = StatusDotType.Confirmed, size = 6.dp)
                    Text(
                        text = stringResource(Res.string.document_sources_independently_verified),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.statusConfirmed,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MobileBankDetailsAccordion(
    bankDetails: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    if (bankDetails.isNullOrBlank()) return

    MobileAccordionCard(
        title = "Bank details",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Text(
            text = bankDetails,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun MobileNotesAccordion(
    notes: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    if (notes.isNullOrBlank()) return

    MobileAccordionCard(
        title = "Notes",
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun MobileAccordionCard(
    title: String,
    count: Int? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    count?.let {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = it.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                        }
                    }
                }
                Text(
                    text = if (expanded) "\u2303" else "\u2304",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun MobileTotalsBlock(
    subtotalAmount: Money?,
    vatAmount: Money?,
    totalAmount: Money?,
    currencySign: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Subtotal ${subtotalAmount?.let { "$currencySign${it.formatAmount()}" } ?: "\u2014"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = "VAT 21% ${vatAmount?.let { "$currencySign${it.formatAmount()}" } ?: "\u2014"}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = "Total $currencySign${totalAmount?.formatAmount() ?: "\u2014"}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
