package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_current
import tech.dokus.aura.resources.contacts_deselect_all
import tech.dokus.aura.resources.contacts_enrichment_apply_all
import tech.dokus.aura.resources.contacts_enrichment_apply_selected_count
import tech.dokus.aura.resources.contacts_enrichment_hint
import tech.dokus.aura.resources.contacts_enrichment_not_now
import tech.dokus.aura.resources.contacts_enrichment_suggestions
import tech.dokus.aura.resources.contacts_select_all
import tech.dokus.features.contacts.mvi.EnrichmentSuggestion
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints

// UI dimension constants
private val SpacingSmall = 4.dp
private val PaddingHorizontal = 6.dp
private val PaddingVertical = 2.dp
private val BadgeCornerRadius = 4.dp
private val CardCornerRadius = 8.dp
private val IconSizeSmall = 12.dp
private val StartPadding = 4.dp
private val TopBottomPadding = 8.dp
private val EndPadding = 12.dp

// Alpha constants
private const val ContainerAlphaDefault = 0.3f

// Confidence thresholds
private const val HighConfidenceThreshold = 0.8f
private const val MediumConfidenceThreshold = 0.6f
private const val PercentageMultiplier = 100

@Composable
internal fun EnrichmentSuggestionsDialog(
    suggestions: List<EnrichmentSuggestion>,
    onApply: (List<EnrichmentSuggestion>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSuggestions =
        remember { mutableStateListOf<EnrichmentSuggestion>().apply { addAll(suggestions) } }

    DokusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.contacts_enrichment_suggestions),
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_enrichment_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.small))

                suggestions.forEach { suggestion ->
                    val isSelected = suggestion in selectedSuggestions

                    EnrichmentSuggestionItem(
                        suggestion = suggestion,
                        isSelected = isSelected,
                        onSelectionChange = { selected ->
                            if (selected) {
                                selectedSuggestions.add(suggestion)
                            } else {
                                selectedSuggestions.remove(suggestion)
                            }
                        }
                    )
                }

                if (suggestions.size > 1) {
                    Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { selectedSuggestions.clear() }
                        ) {
                            Text(
                                text = stringResource(Res.string.contacts_deselect_all),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedSuggestions.clear()
                                selectedSuggestions.addAll(suggestions)
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.contacts_select_all),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = if (selectedSuggestions.size == suggestions.size) {
                stringResource(Res.string.contacts_enrichment_apply_all)
            } else {
                stringResource(
                    Res.string.contacts_enrichment_apply_selected_count,
                    selectedSuggestions.size
                )
            },
            onClick = { onApply(selectedSuggestions.toList()) },
            enabled = selectedSuggestions.isNotEmpty()
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.contacts_enrichment_not_now),
            onClick = onDismiss
        )
    )
}

@Composable
private fun EnrichmentSuggestionItem(
    suggestion: EnrichmentSuggestion,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = ContainerAlphaDefault)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ContainerAlphaDefault)
        },
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = StartPadding, end = EndPadding, top = TopBottomPadding, bottom = TopBottomPadding),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suggestion.field,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    ConfidenceBadge(confidence = suggestion.confidence)
                }

                Text(
                    text = suggestion.suggestedValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (suggestion.currentValue != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_current),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = suggestion.currentValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SourceBadge(source = suggestion.source)
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Float) {
    val percentage = (confidence * PercentageMultiplier).toInt()
    val backgroundColor = when {
        confidence >= HighConfidenceThreshold -> MaterialTheme.colorScheme.tertiaryContainer
        confidence >= MediumConfidenceThreshold -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        confidence >= HighConfidenceThreshold -> MaterialTheme.colorScheme.onTertiaryContainer
        confidence >= MediumConfidenceThreshold -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(BadgeCornerRadius)
    ) {
        Text(
            text = stringResource(Res.string.common_percent_value, percentage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = PaddingHorizontal, vertical = PaddingVertical)
        )
    }
}

@Composable
private fun SourceBadge(source: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(BadgeCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PaddingHorizontal, vertical = PaddingVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(IconSizeSmall),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
