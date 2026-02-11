package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_recipient
import tech.dokus.aura.resources.invoice_click_to_change
import tech.dokus.aura.resources.invoice_click_to_select_client
import tech.dokus.domain.model.contact.ContactDto

/**
 * Clickable client section in the invoice document.
 * Shows client name/details or a placeholder to select a client.
 * Includes hover effect to indicate interactivity.
 *
 * NOTE: PEPPOL status warnings are no longer shown here. PEPPOL recipient
 * availability is resolved at send time via PeppolRecipientResolver.
 */
@Composable
fun InvoiceClientSection(
    client: ContactDto?,
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
            text = stringResource(Res.string.invoice_recipient).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )

        if (client != null) {
            // Client selected - show details
            Text(
                text = client.name.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

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
