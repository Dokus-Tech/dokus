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
import androidx.compose.foundation.layout.Spacer
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
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.model.ClientLookupState
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.common.PLookupDropdownSurface
import tech.dokus.foundation.aura.components.common.PLookupField
import tech.dokus.foundation.aura.components.common.PLookupFieldOutline
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.*

private val HeaderSpacing = 8.dp
private val RowPadding = 12.dp
private val AvatarSize = 30.dp
private val DropdownMarginTop = 6.dp
private val SelectedClientAvatarSize = 42.dp
private val SelectedClientAvatarRadius = 11.dp
private val StatusDotSize = 7.dp
private val SelectedClientVerticalPadding = 2.dp
private val SelectedClientNoPeppolBadgePaddingH = 6.dp
private val SelectedClientNoPeppolBadgePaddingV = 2.dp
private val SelectedClientMetaSpacing = 8.dp

@Composable
internal fun InvoiceClientLookup(
    lookupState: ClientLookupState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier,
    selectedClient: ContactDto? = null,
    peppolStatus: PeppolStatusResponse? = null,
    peppolStatusLoading: Boolean = false
) {
    var boxWidthPx by remember { mutableStateOf(0) }
    var boxHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val dropdownOffsetPx = boxHeightPx + with(density) { DropdownMarginTop.roundToPx() }
    val showSelectedClientSummary = selectedClient != null && !lookupState.isExpanded

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
            if (showSelectedClientSummary) {
                SelectedClientSummary(
                    client = selectedClient,
                    peppolStatus = peppolStatus,
                    peppolStatusLoading = peppolStatusLoading,
                    onChange = { onIntent(CreateInvoiceIntent.SetClientLookupExpanded(true)) }
                )
            } else {
                PLookupField(
                    value = lookupState.query,
                    onValueChange = { onIntent(CreateInvoiceIntent.UpdateClientLookupQuery(it)) },
                    placeholder = stringResource(Res.string.invoice_client_lookup_hint),
                    outline = if (lookupState.query.isBlank()) PLookupFieldOutline.Dashed else PLookupFieldOutline.Solid,
                    isSelected = lookupState.query.isNotBlank(),
                    onFocusChanged = { focused ->
                        if (focused) {
                            onIntent(CreateInvoiceIntent.SetClientLookupExpanded(true))
                        } else if (selectedClient == null && lookupState.query.isBlank()) {
                            onIntent(CreateInvoiceIntent.SetClientLookupExpanded(false))
                        }
                    }
                )
            }

            val hasRows =
                lookupState.mergedSuggestions.isNotEmpty() || lookupState.isLoading || !lookupState.errorHint.isNullOrBlank()
            val showDropdown = !showSelectedClientSummary && lookupState.isExpanded && hasRows

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

@Composable
private fun SelectedClientSummary(
    client: ContactDto,
    peppolStatus: PeppolStatusResponse?,
    peppolStatusLoading: Boolean,
    onChange: () -> Unit
) {
    val showNoPeppolBadge = !peppolStatusLoading &&
        peppolStatus != null &&
        !peppolStatus.isFound
    val statusDotColor = when {
        peppolStatusLoading -> MaterialTheme.colorScheme.textMuted
        peppolStatus?.isFound == true -> MaterialTheme.colorScheme.statusConfirmed
        peppolStatus != null -> MaterialTheme.colorScheme.statusWarning
        else -> MaterialTheme.colorScheme.textMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SelectedClientVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonogramAvatar(
            initials = client.name.value.take(2).uppercase(),
            size = SelectedClientAvatarSize,
            radius = SelectedClientAvatarRadius,
            selected = false
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = client.name.value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(StatusDotSize)
                        .clip(MaterialTheme.shapes.small)
                        .background(statusDotColor)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SelectedClientMetaSpacing)
            ) {
                client.toSelectedClientSecondaryLine()?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }

                if (showNoPeppolBadge) {
                    Text(
                        text = stringResource(Res.string.invoice_no_peppol),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(
                                horizontal = SelectedClientNoPeppolBadgePaddingH,
                                vertical = SelectedClientNoPeppolBadgePaddingV
                            )
                    )
                }
            }
        }

        Text(
            text = stringResource(Res.string.invoice_change),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.clickable(onClick = onChange)
        )
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

private fun ContactDto.toSelectedClientSecondaryLine(): String? {
    return vatNumber?.formatted?.takeIf { it.isNotBlank() } ?: toLookupAddressLine()
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

        Spacer(modifier = Modifier.width(2.dp))
    }
}
