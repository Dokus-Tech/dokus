package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.app.screens.settings.components.formatRelativeTime
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.model.PeppolActivityDto
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.DataRowStatus
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun PeppolConnectionSection(
    peppolRegistration: PeppolRegistrationDto?,
    peppolActivity: PeppolActivityDto?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val status = peppolRegistration?.status
    val sectionStatus = when (status) {
        PeppolRegistrationStatus.Active -> DataRowStatus("Compliant", StatusDotType.Confirmed)
        PeppolRegistrationStatus.SendingOnly -> DataRowStatus("Sending Only", StatusDotType.Warning)
        PeppolRegistrationStatus.WaitingTransfer -> DataRowStatus("Awaiting Transfer", StatusDotType.Neutral)
        PeppolRegistrationStatus.Pending -> DataRowStatus("Pending", StatusDotType.Neutral)
        PeppolRegistrationStatus.Failed -> DataRowStatus("Error", StatusDotType.Error)
        PeppolRegistrationStatus.External -> DataRowStatus("External", StatusDotType.Neutral)
        PeppolRegistrationStatus.NotConfigured, null -> DataRowStatus("Not Configured", StatusDotType.Empty)
    }

    // Show PEPPOL ID when collapsed, descriptive text when expanded
    val subtitle = if (!expanded) {
        peppolRegistration?.peppolId ?: "Not configured"
    } else {
        "Electronic invoicing network status"
    }

    SettingsSection(
        title = "PEPPOL Connection",
        subtitle = subtitle,
        status = sectionStatus,
        expanded = expanded,
        onToggle = onToggle,
        primary = true, // Elevated background
    ) {
        if (peppolRegistration != null && status != PeppolRegistrationStatus.NotConfigured) {
            // Participant ID
            DataRow(
                label = "Participant ID",
                value = peppolRegistration.peppolId,
                mono = true,
                locked = true,
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus("Verified", StatusDotType.Confirmed)
                } else null,
            )

            // Access Point
            DataRow(
                label = "Access Point",
                value = "Managed by Dokus",
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus("Connected", StatusDotType.Confirmed)
                } else null,
            )

            // Inbound status
            val inboundStatus = when {
                peppolRegistration.canReceive && peppolActivity?.lastInboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastInboundAt), StatusDotType.Confirmed)
                peppolRegistration.canReceive ->
                    DataRowStatus("Active", StatusDotType.Confirmed)
                status == PeppolRegistrationStatus.SendingOnly ->
                    DataRowStatus("Blocked", StatusDotType.Warning)
                else ->
                    DataRowStatus("Inactive", StatusDotType.Neutral)
            }
            DataRow(
                label = "Inbound",
                value = if (peppolRegistration.canReceive) "Active" else "Inactive",
                status = inboundStatus,
            )

            // Outbound status
            val outboundStatus = when {
                peppolRegistration.canSend && peppolActivity?.lastOutboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastOutboundAt), StatusDotType.Confirmed)
                peppolRegistration.canSend ->
                    DataRowStatus("Active", StatusDotType.Confirmed)
                else ->
                    DataRowStatus("Inactive", StatusDotType.Neutral)
            }
            DataRow(
                label = "Outbound",
                value = if (peppolRegistration.canSend) "Active" else "Inactive",
                status = outboundStatus,
            )

            // Compliance note
            if (status == PeppolRegistrationStatus.Active) {
                Spacer(Modifier.height(Constrains.Spacing.medium))
                Text(
                    text = "Belgium PEPPOL mandate effective January 1, 2026.\nYour business is compliant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        } else {
            // Not configured
            Text(
                text = "PEPPOL e-invoicing is not configured for your workspace.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(Constrains.Spacing.small))
            Text(
                text = "Belgium requires PEPPOL for B2G invoicing from January 1, 2026.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}
