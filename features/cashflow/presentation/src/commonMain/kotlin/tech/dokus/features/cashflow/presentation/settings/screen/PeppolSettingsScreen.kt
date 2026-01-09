package tech.dokus.features.cashflow.presentation.settings.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_delete
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.peppol_activating
import tech.dokus.aura.resources.peppol_activating_hint
import tech.dokus.aura.resources.peppol_connect_title
import tech.dokus.aura.resources.peppol_connected
import tech.dokus.aura.resources.peppol_connected_to
import tech.dokus.aura.resources.peppol_connection_status
import tech.dokus.aura.resources.peppol_delete_settings
import tech.dokus.aura.resources.peppol_delete_warning
import tech.dokus.aura.resources.peppol_managed_by_dokus
import tech.dokus.aura.resources.peppol_more_providers_coming
import tech.dokus.aura.resources.peppol_not_configured
import tech.dokus.aura.resources.peppol_select_provider_hint
import tech.dokus.aura.resources.peppol_settings_title
import tech.dokus.aura.resources.profile_danger_zone
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.features.cashflow.mvi.PeppolSettingsIntent
import tech.dokus.features.cashflow.mvi.PeppolSettingsState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.description
import tech.dokus.foundation.aura.extensions.iconized
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Peppol E-Invoicing settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun PeppolSettingsScreen(
    state: PeppolSettingsState,
    snackbarHostState: SnackbarHostState,
    showDeleteConfirmation: Boolean,
    onIntent: (PeppolSettingsIntent) -> Unit,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.peppol_settings_title)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        PeppolSettingsContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.padding(contentPadding)
        )
    }

    if (showDeleteConfirmation) {
        DokusDialog(
            onDismissRequest = onDeleteDismiss,
            title = stringResource(Res.string.peppol_delete_settings),
            content = {
                Text(
                    text = stringResource(Res.string.peppol_delete_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            primaryAction = DokusDialogAction(
                text = stringResource(Res.string.action_delete),
                onClick = onDeleteConfirm,
                isDestructive = true
            ),
            secondaryAction = DokusDialogAction(
                text = stringResource(Res.string.action_cancel),
                onClick = onDeleteDismiss
            )
        )
    }
}

/**
 * Peppol settings content without scaffold.
 * Simplified version - shows connection status and connect/disconnect buttons.
 * The actual credentials entry is handled in PeppolConnectScreen.
 */
@Composable
fun PeppolSettingsContent(
    state: PeppolSettingsState,
    onIntent: (PeppolSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    when (state) {
        is PeppolSettingsState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is PeppolSettingsState.NotConfigured -> {
            SettingsContent(
                isConnected = false,
                connectedCompany = null,
                isDeleting = false,
                isManagedPeppol = state.isManagedPeppol,
                onIntent = onIntent,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }

        is PeppolSettingsState.Connected -> {
            SettingsContent(
                isConnected = true,
                connectedCompany = state.connectedCompany,
                isDeleting = false,
                isManagedPeppol = state.isManagedPeppol,
                onIntent = onIntent,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }

        is PeppolSettingsState.Deleting -> {
            SettingsContent(
                isConnected = true,
                connectedCompany = null,
                isDeleting = true,
                isManagedPeppol = false, // Self-hosted only, since cloud can't delete
                onIntent = onIntent,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }

        is PeppolSettingsState.Error -> {
            // Show not configured state with error handling
            SettingsContent(
                isConnected = false,
                connectedCompany = null,
                isDeleting = false,
                isManagedPeppol = false,
                onIntent = onIntent,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun SettingsContent(
    isConnected: Boolean,
    connectedCompany: RecommandCompanySummary?,
    isDeleting: Boolean,
    isManagedPeppol: Boolean,
    onIntent: (PeppolSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Connection Status Card
        DokusCard(
            modifier = Modifier.fillMaxWidth(),
            padding = DokusCardPadding.Default,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.peppol_connection_status),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cloud users who aren't connected are "activating" (auto-provisioning)
                    val statusText = when {
                        isConnected -> Res.string.peppol_connected
                        isManagedPeppol -> Res.string.peppol_activating
                        else -> Res.string.peppol_not_configured
                    }
                    val statusColor = when {
                        isConnected -> MaterialTheme.colorScheme.primary
                        isManagedPeppol -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }

                    if (!isConnected && isManagedPeppol) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = statusColor
                        )
                    } else {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = stringResource(statusText),
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }

                // Show "Managed by Dokus" badge for cloud users when connected
                if (isConnected && isManagedPeppol) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.peppol_managed_by_dokus),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Show hint for cloud users when activating
                if (!isConnected && isManagedPeppol) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.peppol_activating_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Show connected company info
                connectedCompany?.let { company ->
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.peppol_connected_to),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = company.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(Res.string.common_vat_value, company.vatNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Provider Selection - Only show for self-hosted tenants when not connected
        // Cloud tenants have automatic Peppol provisioning - no manual configuration needed
        if (!isConnected && !isManagedPeppol) {
            DokusCard(
                modifier = Modifier.fillMaxWidth(),
                padding = DokusCardPadding.Default,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.peppol_connect_title),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.peppol_select_provider_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Provider cards
                    ProviderCard(
                        provider = PeppolProvider.Recommand,
                        icon = PeppolProvider.Recommand.iconized,
                        description = PeppolProvider.Recommand.description,
                        onClick = {
                            onIntent(PeppolSettingsIntent.SelectProvider(PeppolProvider.Recommand))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.peppol_more_providers_coming),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Danger Zone - Delete Settings
        // Only show for self-hosted tenants when connected
        // Cloud tenants cannot disconnect themselves (support-only operation)
        if (isConnected && !isManagedPeppol) {
            DokusCard(
                modifier = Modifier.fillMaxWidth(),
                padding = DokusCardPadding.Default,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.profile_danger_zone),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.peppol_delete_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    POutlinedButton(
                        text = stringResource(Res.string.peppol_delete_settings),
                        onClick = { onIntent(PeppolSettingsIntent.DeleteSettingsClicked) },
                        enabled = !isDeleting,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isDeleting) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProviderCard(
    @Suppress("SameParameterValue") provider: PeppolProvider,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface

    DokusCard(
        modifier = modifier,
        onClick = onClick,
        variant = DokusCardVariant.Soft,
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = provider.localized,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = provider.localized,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
