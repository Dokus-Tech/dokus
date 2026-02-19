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
import tech.dokus.aura.resources.notification_pref_channel_email
import tech.dokus.aura.resources.notification_pref_channel_inapp
import tech.dokus.aura.resources.notification_pref_compliance_hint
import tech.dokus.aura.resources.notification_pref_load_failed
import tech.dokus.aura.resources.notification_pref_peppol_hint
import tech.dokus.aura.resources.notification_pref_required
import tech.dokus.aura.resources.notification_pref_section_billing
import tech.dokus.aura.resources.notification_pref_section_compliance
import tech.dokus.aura.resources.notification_pref_section_peppol
import tech.dokus.aura.resources.notification_pref_subtitle
import tech.dokus.aura.resources.settings_notifications
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.foundation.aura.extensions.localized
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
                        text = state.exception.message
                            ?: stringResource(Res.string.notification_pref_load_failed),
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
        NotificationType.PeppolReceived,
        NotificationType.PeppolSendConfirmed,
        NotificationType.PeppolSendFailed,
    )
    val complianceRows = listOf(
        NotificationType.ComplianceBlocker,
        NotificationType.VatWarning,
    )
    val billingRows = listOf(
        NotificationType.PaymentConfirmed,
        NotificationType.PaymentFailed,
        NotificationType.SubscriptionChanged,
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
                text = stringResource(Res.string.notification_pref_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.large))

            NotificationSection(
                title = stringResource(Res.string.notification_pref_section_peppol),
                expanded = peppolExpanded,
                onToggle = { peppolExpanded = !peppolExpanded },
                rows = peppolRows,
                state = state,
                onIntent = onIntent,
                hint = stringResource(Res.string.notification_pref_peppol_hint)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            NotificationSection(
                title = stringResource(Res.string.notification_pref_section_compliance),
                expanded = complianceExpanded,
                onToggle = { complianceExpanded = !complianceExpanded },
                rows = complianceRows,
                state = state,
                onIntent = onIntent
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            NotificationSection(
                title = stringResource(Res.string.notification_pref_section_billing),
                expanded = billingExpanded,
                onToggle = { billingExpanded = !billingExpanded },
                rows = billingRows,
                state = state,
                onIntent = onIntent
            )

            Spacer(Modifier.height(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.notification_pref_compliance_hint),
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
    rows: List<NotificationType>,
    state: NotificationPreferencesState.Content,
    onIntent: (NotificationPreferencesIntent) -> Unit,
    hint: String? = null,
) {
    SettingsSection(
        title = title,
        expanded = expanded,
        onToggle = onToggle,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
            rows.forEach { type ->
                NotificationPreferenceRow(
                    label = type.localized,
                    preference = state.preferenceFor(type),
                    isUpdating = type in state.updatingTypes,
                    onToggleEmail = { enabled ->
                        onIntent(NotificationPreferencesIntent.ToggleEmail(type, enabled))
                    }
                )
            }
            if (hint != null) {
                Text(
                    text = hint,
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
    val channelLabel = if (preference.emailEnabled) {
        stringResource(Res.string.notification_pref_channel_email)
    } else {
        stringResource(Res.string.notification_pref_channel_inapp)
    }
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
                style = MaterialTheme.typography.bodyLarge,
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
            text = stringResource(Res.string.notification_pref_required),
            style = MaterialTheme.typography.labelSmall,
            color = StatusDotType.Confirmed.toColor()
        )
        LockIcon(tint = StatusDotType.Confirmed.toColor())
    }
}

