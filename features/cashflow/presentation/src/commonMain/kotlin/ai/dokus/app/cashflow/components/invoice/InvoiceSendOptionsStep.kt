package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.InvoiceDeliveryMethod
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen send options step for mobile invoice creation.
 * Shows delivery method selection with "Coming soon" state.
 */
@Composable
fun InvoiceSendOptionsStep(
    formState: CreateInvoiceFormState,
    selectedMethod: InvoiceDeliveryMethod,
    onMethodSelected: (InvoiceDeliveryMethod) -> Unit,
    onBackToEdit: () -> Unit,
    onSaveAsDraft: () -> Unit,
    isSaving: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackToEdit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to invoice",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Send Options",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Invoice summary card
        MobileInvoiceSummaryCard(formState = formState)

        // Peppol warning for Belgian clients
        if (formState.showPeppolWarning) {
            MobilePeppolWarningBanner()
        }

        // Delivery method options
        Text(
            text = "Choose delivery method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        MobileDeliveryMethodOption(
            icon = Icons.Default.PictureAsPdf,
            title = "Export as PDF",
            description = "Download invoice as PDF file",
            isSelected = selectedMethod == InvoiceDeliveryMethod.PDF_EXPORT,
            isEnabled = true,
            comingSoon = false,
            onClick = { onMethodSelected(InvoiceDeliveryMethod.PDF_EXPORT) }
        )

        MobileDeliveryMethodOption(
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

        MobileDeliveryMethodOption(
            icon = Icons.Default.Email,
            title = "Send via Email",
            description = "Send PDF invoice by email",
            isSelected = selectedMethod == InvoiceDeliveryMethod.EMAIL,
            isEnabled = false,
            comingSoon = true,
            onClick = { onMethodSelected(InvoiceDeliveryMethod.EMAIL) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Info text
        Text(
            text = "Sending features are coming soon. For now, save your invoice as a draft.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Save as Draft button
        PButton(
            text = "Save as Draft",
            variant = PButtonVariant.Default,
            onClick = onSaveAsDraft,
            isEnabled = formState.isValid && !isSaving,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MobileInvoiceSummaryCard(
    formState: CreateInvoiceFormState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Invoice Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryRow(
                label = "Client",
                value = formState.selectedClient?.name?.value ?: "Not selected"
            )

            SummaryRow(
                label = "Issue Date",
                value = formState.issueDate?.toString() ?: "Not set"
            )

            SummaryRow(
                label = "Due Date",
                value = formState.dueDate?.toString() ?: "Not set"
            )

            SummaryRow(
                label = "Items",
                value = "${formState.items.count { it.isValid }} line item(s)"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formState.total,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MobilePeppolWarningBanner(
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
private fun MobileDeliveryMethodOption(
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                when {
                    isSelected && isEnabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                shape = MaterialTheme.shapes.medium
            )
            .then(
                if (isEnabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
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
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )

                if (comingSoon) {
                    MobileComingSoonBadge()
                }

                if (hasWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
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
private fun MobileComingSoonBadge(
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
