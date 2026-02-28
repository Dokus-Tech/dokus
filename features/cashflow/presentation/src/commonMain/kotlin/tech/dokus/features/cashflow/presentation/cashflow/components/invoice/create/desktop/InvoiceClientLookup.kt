package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.model.ClientLookupState
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.foundation.aura.components.common.PLookupDropdownSurface
import tech.dokus.foundation.aura.components.common.PLookupField
import tech.dokus.foundation.aura.components.common.PLookupFieldOutline
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val HeaderSpacing = 8.dp
private val RowPadding = 12.dp
private val AvatarSize = 30.dp
private val FieldHeight = 56.dp
private val DropdownMarginTop = 6.dp

@Composable
internal fun InvoiceClientLookup(
    lookupState: ClientLookupState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing)
    ) {
        Text(
            text = "BILL TO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textMuted
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            PLookupField(
                value = lookupState.query,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateClientLookupQuery(it)) },
                placeholder = "Name, VAT, or email\u2026",
                outline = if (lookupState.query.isBlank()) PLookupFieldOutline.Dashed else PLookupFieldOutline.Solid,
                isSelected = lookupState.query.isNotBlank(),
                onFocusChanged = { focused ->
                    onIntent(
                        CreateInvoiceIntent.SetClientLookupExpanded(
                            focused && lookupState.query.isNotBlank()
                        )
                    )
                }
            )

            val hasRows = lookupState.mergedSuggestions.isNotEmpty() || lookupState.isLoading || !lookupState.errorHint.isNullOrBlank()
            val showDropdown = lookupState.isExpanded && hasRows
            if (showDropdown) {
                PLookupDropdownSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FieldHeight + DropdownMarginTop)
                        .align(Alignment.TopStart),
                    footer = {
                        lookupState.errorHint?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        if (lookupState.isLoading) {
                            item {
                                Text(
                                    text = "Searching...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textMuted,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        items(lookupState.mergedSuggestions) { suggestion ->
                            when (suggestion) {
                                is ClientSuggestion.LocalContact -> {
                                    LookupSuggestionRow(
                                        title = suggestion.contact.name.value,
                                        subtitle = suggestion.contact.vatNumber?.formatted
                                            ?: suggestion.contact.email?.value,
                                        leading = suggestion.contact.name.value.take(2).uppercase(),
                                        cbeBadge = false,
                                        peppolEnabled = true,
                                        onClick = {
                                            onIntent(CreateInvoiceIntent.SelectClient(suggestion.contact))
                                            onIntent(CreateInvoiceIntent.SetClientLookupExpanded(false))
                                        }
                                    )
                                }

                                is ClientSuggestion.ExternalCompany -> {
                                    LookupSuggestionRow(
                                        title = suggestion.candidate.name,
                                        subtitle = suggestion.candidate.vatNumber?.formatted
                                            ?: suggestion.candidate.enterpriseNumber,
                                        leading = suggestion.candidate.name.take(2).uppercase(),
                                        cbeBadge = true,
                                        peppolEnabled = suggestion.candidate.vatNumber != null,
                                        onClick = {
                                            onIntent(CreateInvoiceIntent.SelectExternalClientCandidate(suggestion.candidate))
                                            onIntent(CreateInvoiceIntent.SetClientLookupExpanded(false))
                                        }
                                    )
                                }

                                is ClientSuggestion.CreateManual -> {
                                    LookupSuggestionRow(
                                        title = "Create manually",
                                        subtitle = "Individuals, foreign, NGOs",
                                        leading = "+",
                                        cbeBadge = false,
                                        peppolEnabled = false,
                                        onClick = {
                                            onIntent(CreateInvoiceIntent.CreateClientManuallyFromQuery(suggestion.query))
                                            onIntent(CreateInvoiceIntent.SetClientLookupExpanded(false))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupSuggestionRow(
    title: String,
    subtitle: String?,
    leading: String,
    cbeBadge: Boolean,
    peppolEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = leading,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                if (cbeBadge) {
                    Text(
                        text = "CBE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    if (peppolEnabled) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
        )
    }
}
