package tech.dokus.app.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.NotificationPreferencesIntent
import tech.dokus.app.viewmodel.NotificationPreferencesState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_notifications
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.icons.LockIcon
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.status.toColor
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

private val MaxContentWidth = 820.dp

@Composable
internal fun NotificationPreferencesScreen(
    state: NotificationPreferencesState,
    snackbarHostState: SnackbarHostState,
    onIntent: (NotificationPreferencesIntent) -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Scaffold(
        topBar = {
            if (!isLargeScreen) {
                PTopAppBar(Res.string.settings_notifications)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        NotificationPreferencesContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

@Composable
internal fun NotificationPreferencesContent(
    state: NotificationPreferencesState,
    onIntent: (NotificationPreferencesIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    when (state) {
        NotificationPreferencesState.Loading -> {
            Box(
                modifier = modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }

        is NotificationPreferencesState.Content -> {
            NotificationPreferencesContentScreen(
                state = state,
                onIntent = onIntent,
                modifier = modifier.padding(contentPadding)
            )
        }

        is NotificationPreferencesState.Error -> {
            Box(
                modifier = modifier
                    .padding(contentPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.exception.message ?: "Failed to load notification preferences",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    PPrimaryButton(
                        text = stringResource(Res.string.state_retry),
                        onClick = { onIntent(NotificationPreferencesIntent.Load) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferencesContentScreen(
    state: NotificationPreferencesState.Content,
    onIntent: (NotificationPreferencesIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var peppolExpanded by remember { mutableStateOf(true) }
    var complianceExpanded by remember { mutableStateOf(false) }
    var billingExpanded by remember { mutableStateOf(false) }

    val peppolRows = listOf(
        NotificationType.PeppolReceived to "New document received",
        NotificationType.PeppolSendConfirmed to "Send confirmed",
        NotificationType.PeppolSendFailed to "Send failed",
    )
    val complianceRows = listOf(
        NotificationType.ComplianceBlocker to "Blocking issues",
        NotificationType.VatWarning to "VAT warnings",
    )
    val billingRows = listOf(
        NotificationType.PaymentConfirmed to "Payment confirmed",
        NotificationType.PaymentFailed to "Payment failed",
        NotificationType.SubscriptionChanged to "Subscription changed",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .withContentPaddingForScrollable(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.settings_notifications),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(Constrains.Spacing.xxSmall))
            Text(
                text = "In-app notifications are always on. Configure email per event.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.large))

            NotificationSection(
                title = "PEPPOL Events",
                expanded = peppolExpanded,
                onToggle = { peppolExpanded = !peppolExpanded },
                rows = peppolRows,
                state = state,
                onIntent = onIntent
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            NotificationSection(
                title = "Compliance Alerts",
                expanded = complianceExpanded,
                onToggle = { complianceExpanded = !complianceExpanded },
                rows = complianceRows,
                state = state,
                onIntent = onIntent
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            NotificationSection(
                title = "Billing",
                expanded = billingExpanded,
                onToggle = { billingExpanded = !billingExpanded },
                rows = billingRows,
                state = state,
                onIntent = onIntent
            )

            Spacer(Modifier.height(Constrains.Spacing.small))
            Text(
                text = "Some alerts are required for compliance and cannot be disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.xLarge))
        }
    }
}

@Composable
private fun NotificationSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    rows: List<Pair<NotificationType, String>>,
    state: NotificationPreferencesState.Content,
    onIntent: (NotificationPreferencesIntent) -> Unit,
) {
    SettingsSection(
        title = title,
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
            rows.forEach { (type, label) ->
                NotificationPreferenceRow(
                    label = label,
                    preference = state.preferenceFor(type),
                    isUpdating = type in state.updatingTypes,
                    onToggleEmail = { enabled ->
                        onIntent(NotificationPreferencesIntent.ToggleEmail(type, enabled))
                    }
                )
            }
            if (title == "PEPPOL Events") {
                Text(
                    text = "PEPPOL transmission failures have legal and compliance implications. Email notifications cannot be disabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.padding(top = Constrains.Spacing.xSmall)
                )
            }
        }
    }
}

@Composable
private fun NotificationPreferenceRow(
    label: String,
    preference: NotificationPreferenceDto,
    isUpdating: Boolean,
    onToggleEmail: (Boolean) -> Unit,
) {
    val channelLabel = if (preference.emailEnabled) "In-app + Email" else "In-app only"
    val isLarge = LocalScreenSize.current.isLarge

    if (isLarge) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Constrains.Spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.5f)
            )
            Text(
                text = channelLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.weight(0.3f)
            )
            Box(
                modifier = Modifier.weight(0.2f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (preference.emailLocked) {
                    RequiredBadge()
                } else {
                    Switch(
                        checked = preference.emailEnabled,
                        enabled = !isUpdating,
                        onCheckedChange = onToggleEmail
                    )
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Constrains.Spacing.xxSmall),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xxSmall)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channelLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.weight(1f)
                )
                if (preference.emailLocked) {
                    RequiredBadge()
                } else {
                    Switch(
                        checked = preference.emailEnabled,
                        enabled = !isUpdating,
                        onCheckedChange = onToggleEmail
                    )
                }
            }
        }
    }
}

@Composable
private fun RequiredBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
    ) {
        StatusDot(type = StatusDotType.Confirmed)
        Text(
            text = "Required",
            style = MaterialTheme.typography.labelSmall,
            color = StatusDotType.Confirmed.toColor()
        )
        LockIcon(tint = StatusDotType.Confirmed.toColor())
    }
}

