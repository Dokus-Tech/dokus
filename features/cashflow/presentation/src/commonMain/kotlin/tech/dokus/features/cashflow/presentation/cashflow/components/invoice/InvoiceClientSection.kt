package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_bill_to
import tech.dokus.aura.resources.invoice_click_to_change
import tech.dokus.aura.resources.invoice_click_to_select_client
import tech.dokus.aura.resources.peppol_belgian_client_warning_short
import tech.dokus.aura.resources.peppol_id_missing
import tech.dokus.domain.model.contact.ContactDto
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * Clickable client section in the invoice document.
 * Shows client name/details or a placeholder to select a client.
 * Includes hover effect to indicate interactivity.
 */
@Composable
fun InvoiceClientSection(
    client: ContactDto?,
    showPeppolWarning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isHovered) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.invoice_bill_to).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        if (client != null) {
            // Client selected - show details
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (showPeppolWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(Res.string.peppol_id_missing),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Show additional client info
            client.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Show address if available
            val addressParts = listOfNotNull(
                client.addressLine1,
                listOfNotNull(client.postalCode, client.city).takeIf { it.isNotEmpty() }?.joinToString(" "),
                client.country
            )
            if (addressParts.isNotEmpty()) {
                Text(
                    text = addressParts.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Show Peppol warning message
            if (showPeppolWarning) {
                Text(
                    text = stringResource(Res.string.peppol_belgian_client_warning_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            // No client selected - show placeholder
            Text(
                text = stringResource(Res.string.invoice_click_to_select_client),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hover hint
        if (isHovered) {
            Text(
                text = stringResource(Res.string.invoice_click_to_change),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}
