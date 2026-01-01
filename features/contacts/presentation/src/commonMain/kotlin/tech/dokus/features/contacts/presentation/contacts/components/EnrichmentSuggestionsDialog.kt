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
import androidx.compose.material3.AlertDialog
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

@Composable
internal fun EnrichmentSuggestionsDialog(
    suggestions: List<EnrichmentSuggestion>,
    onApply: (List<EnrichmentSuggestion>) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedSuggestions =
        remember { mutableStateListOf<EnrichmentSuggestion>().apply { addAll(suggestions) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.contacts_enrichment_suggestions),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_enrichment_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    Spacer(modifier = Modifier.height(4.dp))
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
        confirmButton = {
            TextButton(
                onClick = { onApply(selectedSuggestions.toList()) },
                enabled = selectedSuggestions.isNotEmpty()
            ) {
                Text(
                    text = if (selectedSuggestions.size == suggestions.size) {
                        stringResource(Res.string.contacts_enrichment_apply_all)
                    } else {
                        stringResource(
                            Res.string.contacts_enrichment_apply_selected_count,
                            selectedSuggestions.size
                        )
                    },
                    fontWeight = FontWeight.Medium,
                    color = if (selectedSuggestions.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.contacts_enrichment_not_now),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
    val percentage = (confidence * 100).toInt()
    val backgroundColor = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.tertiaryContainer
        confidence >= 0.6f -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.onTertiaryContainer
        confidence >= 0.6f -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.common_percent_value, percentage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun SourceBadge(source: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
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
