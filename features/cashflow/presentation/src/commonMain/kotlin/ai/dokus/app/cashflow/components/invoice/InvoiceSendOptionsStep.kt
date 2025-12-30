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
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_back_to_edit
import tech.dokus.aura.resources.invoice_choose_delivery_method
import tech.dokus.aura.resources.invoice_client
import tech.dokus.aura.resources.invoice_coming_soon
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_items
import tech.dokus.aura.resources.invoice_line_items_count
import tech.dokus.aura.resources.invoice_not_selected
import tech.dokus.aura.resources.invoice_not_set
import tech.dokus.aura.resources.invoice_save_as_draft
import tech.dokus.aura.resources.invoice_send_coming_soon_message
import tech.dokus.aura.resources.invoice_send_options
import tech.dokus.aura.resources.invoice_summary
import tech.dokus.aura.resources.invoice_total
import tech.dokus.aura.resources.peppol_belgian_client_warning
import tech.dokus.aura.resources.peppol_id_missing
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

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
                    contentDescription = stringResource(Res.string.invoice_back_to_edit),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(Res.string.invoice_send_options),
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
            text = stringResource(Res.string.invoice_choose_delivery_method),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        DeliveryMethodOption.all(showPeppolWarning = formState.showPeppolWarning).forEach { option ->
            MobileDeliveryMethodOptionRow(
                option = option,
                isSelected = selectedMethod == option.deliveryMethod,
                onClick = { onMethodSelected(option.deliveryMethod) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Info text
        Text(
            text = stringResource(Res.string.invoice_send_coming_soon_message),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Save as Draft button
        PButton(
            text = stringResource(Res.string.invoice_save_as_draft),
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
                text = stringResource(Res.string.invoice_summary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SummaryRow(
                label = stringResource(Res.string.invoice_client),
                value = formState.selectedClient?.name?.value ?: stringResource(Res.string.invoice_not_selected)
            )

            SummaryRow(
                label = stringResource(Res.string.invoice_issue_date),
                value = formState.issueDate?.toString() ?: stringResource(Res.string.invoice_not_set)
            )

            SummaryRow(
                label = stringResource(Res.string.invoice_due_date),
                value = formState.dueDate?.toString() ?: stringResource(Res.string.invoice_not_set)
            )

            SummaryRow(
                label = stringResource(Res.string.invoice_items),
                value = stringResource(
                    Res.string.invoice_line_items_count,
                    formState.items.count { it.isValid }
                )
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.invoice_total),
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
private fun MobileDeliveryMethodOptionRow(
    option: DeliveryMethodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = option.isEnabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                when {
                    isSelected && enabled -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
                shape = MaterialTheme.shapes.medium
            )
            .then(
                if (enabled) {
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
            modifier = Modifier.size(28.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.localized,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )

                if (option.isComingSoon) {
                    MobileComingSoonBadge()
                }

                if (option.hasWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
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
            text = stringResource(Res.string.invoice_coming_soon),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            letterSpacing = 0.5.sp
        )
    }
}
