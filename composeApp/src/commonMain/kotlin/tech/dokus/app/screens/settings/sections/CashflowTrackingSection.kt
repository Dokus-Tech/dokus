package tech.dokus.app.screens.settings.sections

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_cashflow_tracking_from
import tech.dokus.aura.resources.workspace_cashflow_tracking_subtitle
import tech.dokus.aura.resources.workspace_cashflow_tracking_title
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.SettingsSection

@Composable
internal fun CashflowTrackingSection(
    trackingFrom: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    SettingsSection(
        title = stringResource(Res.string.workspace_cashflow_tracking_title),
        subtitle = stringResource(Res.string.workspace_cashflow_tracking_subtitle),
        expanded = expanded,
        onToggle = onToggle,
        primary = false,
    ) {
        DataRow(
            label = stringResource(Res.string.workspace_cashflow_tracking_from),
            value = trackingFrom,
        )
    }
}
