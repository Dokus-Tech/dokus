package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.screens.settings.components.formatRelativeTime
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_conn_compliant_note
import tech.dokus.aura.resources.peppol_conn_label_access_point
import tech.dokus.aura.resources.peppol_conn_label_inbound
import tech.dokus.aura.resources.peppol_conn_label_outbound
import tech.dokus.aura.resources.peppol_conn_label_participant_id
import tech.dokus.aura.resources.peppol_conn_mandate_note
import tech.dokus.aura.resources.peppol_conn_not_configured_text
import tech.dokus.aura.resources.peppol_conn_status_active
import tech.dokus.aura.resources.peppol_conn_status_awaiting_transfer
import tech.dokus.aura.resources.peppol_conn_status_blocked
import tech.dokus.aura.resources.peppol_conn_status_compliant
import tech.dokus.aura.resources.peppol_conn_status_connected
import tech.dokus.aura.resources.peppol_conn_status_error
import tech.dokus.aura.resources.peppol_conn_status_external
import tech.dokus.aura.resources.peppol_conn_status_inactive
import tech.dokus.aura.resources.peppol_conn_status_pending
import tech.dokus.aura.resources.peppol_conn_status_sending_only
import tech.dokus.aura.resources.peppol_conn_status_verified
import tech.dokus.aura.resources.peppol_conn_subtitle_expanded
import tech.dokus.aura.resources.peppol_conn_title
import tech.dokus.aura.resources.peppol_managed_by_dokus
import tech.dokus.aura.resources.peppol_not_configured
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
        PeppolRegistrationStatus.Active -> DataRowStatus(stringResource(Res.string.peppol_conn_status_compliant), StatusDotType.Confirmed)
        PeppolRegistrationStatus.SendingOnly -> DataRowStatus(stringResource(Res.string.peppol_conn_status_sending_only), StatusDotType.Warning)
        PeppolRegistrationStatus.WaitingTransfer -> DataRowStatus(stringResource(Res.string.peppol_conn_status_awaiting_transfer), StatusDotType.Neutral)
        PeppolRegistrationStatus.Pending -> DataRowStatus(stringResource(Res.string.peppol_conn_status_pending), StatusDotType.Neutral)
        PeppolRegistrationStatus.Failed -> DataRowStatus(stringResource(Res.string.peppol_conn_status_error), StatusDotType.Error)
        PeppolRegistrationStatus.External -> DataRowStatus(stringResource(Res.string.peppol_conn_status_external), StatusDotType.Neutral)
        PeppolRegistrationStatus.NotConfigured, null -> DataRowStatus(stringResource(Res.string.peppol_not_configured), StatusDotType.Empty)
    }

    // Show PEPPOL ID when collapsed, descriptive text when expanded
    val subtitle = if (!expanded) {
        peppolRegistration?.peppolId ?: stringResource(Res.string.peppol_not_configured)
    } else {
        stringResource(Res.string.peppol_conn_subtitle_expanded)
    }

    val activeLabel = stringResource(Res.string.peppol_conn_status_active)
    val inactiveLabel = stringResource(Res.string.peppol_conn_status_inactive)

    SettingsSection(
        title = stringResource(Res.string.peppol_conn_title),
        subtitle = subtitle,
        status = sectionStatus,
        expanded = expanded,
        onToggle = onToggle,
        primary = true, // Elevated background
    ) {
        if (peppolRegistration != null && status != PeppolRegistrationStatus.NotConfigured) {
            // Participant ID
            DataRow(
                label = stringResource(Res.string.peppol_conn_label_participant_id),
                value = peppolRegistration.peppolId,
                mono = true,
                locked = true,
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus(stringResource(Res.string.peppol_conn_status_verified), StatusDotType.Confirmed)
                } else null,
            )

            // Access Point
            DataRow(
                label = stringResource(Res.string.peppol_conn_label_access_point),
                value = stringResource(Res.string.peppol_managed_by_dokus),
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus(stringResource(Res.string.peppol_conn_status_connected), StatusDotType.Confirmed)
                } else null,
            )

            // Inbound status
            val inboundStatus = when {
                peppolRegistration.canReceive && peppolActivity?.lastInboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastInboundAt), StatusDotType.Confirmed)
                peppolRegistration.canReceive ->
                    DataRowStatus(activeLabel, StatusDotType.Confirmed)
                status == PeppolRegistrationStatus.SendingOnly ->
                    DataRowStatus(stringResource(Res.string.peppol_conn_status_blocked), StatusDotType.Warning)
                else ->
                    DataRowStatus(inactiveLabel, StatusDotType.Neutral)
            }
            DataRow(
                label = stringResource(Res.string.peppol_conn_label_inbound),
                value = if (peppolRegistration.canReceive) activeLabel else inactiveLabel,
                status = inboundStatus,
            )

            // Outbound status
            val outboundStatus = when {
                peppolRegistration.canSend && peppolActivity?.lastOutboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastOutboundAt), StatusDotType.Confirmed)
                peppolRegistration.canSend ->
                    DataRowStatus(activeLabel, StatusDotType.Confirmed)
                else ->
                    DataRowStatus(inactiveLabel, StatusDotType.Neutral)
            }
            DataRow(
                label = stringResource(Res.string.peppol_conn_label_outbound),
                value = if (peppolRegistration.canSend) activeLabel else inactiveLabel,
                status = outboundStatus,
            )

            // Compliance note
            if (status == PeppolRegistrationStatus.Active) {
                Spacer(Modifier.height(Constrains.Spacing.medium))
                Text(
                    text = stringResource(Res.string.peppol_conn_compliant_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        } else {
            // Not configured
            Text(
                text = stringResource(Res.string.peppol_conn_not_configured_text),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.peppol_conn_mandate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}
