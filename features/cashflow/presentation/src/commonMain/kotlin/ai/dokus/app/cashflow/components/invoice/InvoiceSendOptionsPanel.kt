package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.DeliveryMethodOption
import ai.dokus.app.cashflow.viewmodel.model.InvoiceDeliveryMethod
import ai.dokus.app.cashflow.viewmodel.deliveryMethod
import ai.dokus.app.cashflow.viewmodel.iconized
import ai.dokus.app.cashflow.viewmodel.isComingSoon
import ai.dokus.app.cashflow.viewmodel.isEnabled
import ai.dokus.app.cashflow.viewmodel.localized
import ai.dokus.app.cashflow.viewmodel.localizedDescription
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.invoice_client
import ai.dokus.app.resources.generated.invoice_coming_soon
import ai.dokus.app.resources.generated.invoice_items
import ai.dokus.app.resources.generated.invoice_line_items_count
import ai.dokus.app.resources.generated.invoice_not_selected
import ai.dokus.app.resources.generated.invoice_save_as_draft
import ai.dokus.app.resources.generated.invoice_send_coming_soon_message
import ai.dokus.app.resources.generated.invoice_send_options
import ai.dokus.app.resources.generated.invoice_summary
import ai.dokus.app.resources.generated.invoice_total
import ai.dokus.app.resources.generated.peppol_belgian_client_warning
import ai.dokus.app.resources.generated.peppol_id_missing
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

/**
 * Side panel for send options in the invoice creation flow (desktop).
 * Shows delivery method selection with "Coming soon" state.
 */
@Composable
fun InvoiceSendOptionsPanel(
    formState: CreateInvoiceFormState,
    selectedMethod: InvoiceDeliveryMethod,
    onMethodSelected: (InvoiceDeliveryMethod) -> Unit,
    onSaveAsDraft: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.invoice_send_options),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Peppol warning for Belgian clients
            if (formState.showPeppolWarning) {
                PeppolWarningBanner()
            }

            // Delivery method options
            DeliveryMethodOption.all(showPeppolWarning = formState.showPeppolWarning).forEach { option ->
                DeliveryMethodOptionRow(
                    option = option,
                    isSelected = selectedMethod == option.deliveryMethod,
                    onClick = { onMethodSelected(option.deliveryMethod) }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Invoice summary
            InvoiceMiniSummary(formState = formState)

            Spacer(modifier = Modifier.height(8.dp))

            // Save as Draft button
            PButton(
                text = stringResource(Res.string.invoice_save_as_draft),
                variant = PButtonVariant.Default,
                onClick = onSaveAsDraft,
                isEnabled = formState.isValid && !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            // Info text
            Text(
                text = stringResource(Res.string.invoice_send_coming_soon_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeppolWarningBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = stringResource(Res.string.peppol_id_missing),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(Res.string.peppol_belgian_client_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DeliveryMethodOptionRow(
    option: DeliveryMethodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val enabled = option.isEnabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected && enabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    isHovered && enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected && enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.small
            )
            .then(
                if (enabled) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                        .pointerHoverIcon(PointerIcon.Hand)
                } else {
                    Modifier
                }
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = if (enabled) onClick else null,
            enabled = enabled
        )

        Icon(
            imageVector = option.iconized,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            modifier = Modifier.size(24.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.localized,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )

                if (option.isComingSoon) {
                    ComingSoonBadge()
                }

                if (option.hasWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = option.localizedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (option.hasWarning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ComingSoonBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(Res.string.invoice_coming_soon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun InvoiceMiniSummary(
    formState: CreateInvoiceFormState,
    modifier: Modifier = Modifier
) {
    val itemCount = formState.items.count { it.isValid }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.invoice_summary),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_client),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formState.selectedClient?.name?.value ?: stringResource(Res.string.invoice_not_selected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_items),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.invoice_line_items_count, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.invoice_total),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formState.total,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
fun InvoiceSendOptionsPanelPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceSendOptionsPanel(
            formState = Mocks.sampleFormState,
            selectedMethod = InvoiceDeliveryMethod.PDF_EXPORT,
            onMethodSelected = {},
            onSaveAsDraft = {},
            isSaving = false,
            modifier = Modifier.padding(16.dp)
        )
    }
}
