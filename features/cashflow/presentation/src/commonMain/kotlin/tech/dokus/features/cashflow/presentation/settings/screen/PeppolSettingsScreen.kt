package tech.dokus.features.cashflow.presentation.settings.screen

import tech.dokus.features.cashflow.mvi.PeppolSettingsIntent
import tech.dokus.features.cashflow.mvi.PeppolSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_delete
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.peppol_connect_title
import tech.dokus.aura.resources.peppol_connected
import tech.dokus.aura.resources.peppol_connected_to
import tech.dokus.aura.resources.peppol_connection_status
import tech.dokus.aura.resources.peppol_delete_settings
import tech.dokus.aura.resources.peppol_delete_warning
import tech.dokus.aura.resources.peppol_more_providers_coming
import tech.dokus.aura.resources.peppol_not_configured
import tech.dokus.aura.resources.peppol_provider_recommand_description
import tech.dokus.aura.resources.peppol_select_provider_hint
import tech.dokus.aura.resources.peppol_settings_title
import tech.dokus.aura.resources.profile_danger_zone
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.domain.model.PeppolProvider
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
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.model.RecommandCompanySummary

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
    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.peppol_settings_title)
            )
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
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = {
                Text(text = stringResource(Res.string.peppol_delete_settings))
            },
            text = {
                Text(text = stringResource(Res.string.peppol_delete_warning))
            },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirm
                ) {
                    Text(text = stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDeleteDismiss
                ) {
                    Text(text = stringResource(Res.string.action_cancel))
                }
            }
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
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isConnected) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(
                            if (isConnected) Res.string.peppol_connected
                            else Res.string.peppol_not_configured
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isConnected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
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

        // Provider Selection - Only show if not connected
        if (!isConnected) {
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
                        icon = Icons.Outlined.Receipt,
                        description = stringResource(Res.string.peppol_provider_recommand_description),
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

        // Danger Zone - Delete Settings (only when connected)
        if (isConnected) {
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
    provider: PeppolProvider,
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
                contentDescription = provider.displayName,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = provider.displayName,
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
