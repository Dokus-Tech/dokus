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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.model.ClientLookupState
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.foundation.aura.components.common.PLookupDropdownSurface
import tech.dokus.foundation.aura.components.common.PLookupField
import tech.dokus.foundation.aura.components.common.PLookupFieldOutline
import tech.dokus.foundation.aura.style.textMuted
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.*

private val HeaderSpacing = 8.dp
private val RowPadding = 12.dp
private val AvatarSize = 30.dp
private val DropdownMarginTop = 6.dp

@Composable
internal fun InvoiceClientLookup(
    lookupState: ClientLookupState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxWidthPx by remember { mutableStateOf(0) }
    var boxHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val dropdownOffsetPx = boxHeightPx + with(density) { DropdownMarginTop.roundToPx() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing)
    ) {
        Text(
            text = stringResource(Res.string.invoice_bill_to_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textMuted
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    boxWidthPx = coordinates.size.width
                    boxHeightPx = coordinates.size.height
                }
        ) {
            PLookupField(
                value = lookupState.query,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateClientLookupQuery(it)) },
                placeholder = stringResource(Res.string.invoice_client_lookup_hint),
                outline = if (lookupState.query.isBlank()) PLookupFieldOutline.Dashed else PLookupFieldOutline.Solid,
                isSelected = lookupState.query.isNotBlank(),
                onFocusChanged = { focused ->
                    onIntent(CreateInvoiceIntent.SetClientLookupExpanded(focused))
                }
            )

            val hasRows =
                lookupState.mergedSuggestions.isNotEmpty() || lookupState.isLoading || !lookupState.errorHint.isNullOrBlank()
            val showDropdown = lookupState.isExpanded && hasRows

            if (showDropdown) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, dropdownOffsetPx),
                    onDismissRequest = {
                        onIntent(CreateInvoiceIntent.SetClientLookupExpanded(false))
                    },
                    properties = PopupProperties(focusable = false)
                ) {
                    PLookupDropdownSurface(
                        modifier = Modifier.width(with(density) { boxWidthPx.toDp() }),
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
                                        text = stringResource(Res.string.invoice_searching),
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
                                            subtitle = suggestion.contact.toLookupAddressLine(),
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
                                            subtitle = suggestion.candidate.toLookupAddressLine(),
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
                                            title = stringResource(Res.string.invoice_create_manually),
                                            subtitle = stringResource(Res.string.invoice_create_manually_desc),
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
}

private fun ContactDto.toLookupAddressLine(): String? {
    val address = listOfNotNull(
        addressLine1?.takeIf { it.isNotBlank() },
        addressLine2?.takeIf { it.isNotBlank() },
        listOfNotNull(
            postalCode?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() }
        ).takeIf { it.isNotEmpty() }?.joinToString(" "),
        country?.takeIf { it.isNotBlank() }
    ).joinToString(", ")

    return address.ifBlank { email?.value }
}

private fun ExternalClientCandidate.toLookupAddressLine(): String? {
    return prefillAddress?.takeIf { it.isNotBlank() } ?: enterpriseNumber
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
                        text = stringResource(Res.string.invoice_cbe_badge),
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
