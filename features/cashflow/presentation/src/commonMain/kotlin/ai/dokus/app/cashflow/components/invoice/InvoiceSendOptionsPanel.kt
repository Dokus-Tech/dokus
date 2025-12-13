package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.InvoiceDeliveryMethod
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                text = "Send Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Peppol warning for Belgian clients
            if (formState.showPeppolWarning) {
                PeppolWarningBanner()
            }

            // Delivery method options
            DeliveryMethodOption(
                icon = Icons.Default.PictureAsPdf,
                title = "Export as PDF",
                description = "Download invoice as PDF file",
                isSelected = selectedMethod == InvoiceDeliveryMethod.PDF_EXPORT,
                isEnabled = true,
                comingSoon = false,
                onClick = { onMethodSelected(InvoiceDeliveryMethod.PDF_EXPORT) }
            )

            DeliveryMethodOption(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Send via Peppol",
                description = if (formState.showPeppolWarning) {
                    "Client does not have Peppol ID configured"
                } else {
                    "Send e-invoice directly to client"
                },
                isSelected = selectedMethod == InvoiceDeliveryMethod.PEPPOL,
                isEnabled = false,
                comingSoon = true,
                hasWarning = formState.showPeppolWarning,
                onClick = { onMethodSelected(InvoiceDeliveryMethod.PEPPOL) }
            )

            DeliveryMethodOption(
                icon = Icons.Default.Email,
                title = "Send via Email",
                description = "Send PDF invoice by email",
                isSelected = selectedMethod == InvoiceDeliveryMethod.EMAIL,
                isEnabled = false,
                comingSoon = true,
                onClick = { onMethodSelected(InvoiceDeliveryMethod.EMAIL) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Invoice summary
            InvoiceMiniSummary(formState = formState)

            Spacer(modifier = Modifier.height(8.dp))

            // Save as Draft button
            PButton(
                text = "Save as Draft",
                variant = PButtonVariant.Default,
                onClick = onSaveAsDraft,
                isEnabled = formState.isValid && !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            // Info text
            Text(
                text = "Sending features are coming soon. For now, save your invoice as a draft.",
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
                text = "Peppol ID Missing",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "This Belgian client needs a Peppol ID for e-invoicing (mandatory from 2026)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun DeliveryMethodOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    comingSoon: Boolean,
    hasWarning: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected && isEnabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    isHovered && isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .border(
                width = 1.dp,
                color = if (isSelected && isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = MaterialTheme.shapes.small
            )
            .then(
                if (isEnabled) {
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
            onClick = if (isEnabled) onClick else null,
            enabled = isEnabled
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled) {
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
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )

                if (comingSoon) {
                    ComingSoonBadge()
                }

                if (hasWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasWarning) {
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
            text = "Coming soon",
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Invoice Summary",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Client",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formState.selectedClient?.name?.value ?: "Not selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${formState.items.count { it.isValid }} line item(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total",
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
