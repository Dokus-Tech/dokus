package ai.dokus.app.cashflow.screens.settings

import ai.dokus.app.cashflow.viewmodel.PeppolSettingsAction
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsContainer
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsIntent
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.peppol_connected
import ai.dokus.app.resources.generated.peppol_connection_status
import ai.dokus.app.resources.generated.peppol_delete_settings
import ai.dokus.app.resources.generated.peppol_not_configured
import ai.dokus.app.resources.generated.peppol_settings_title
import ai.dokus.app.resources.generated.profile_danger_zone
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import tech.dokus.domain.model.PeppolProvider
import ai.dokus.foundation.navigation.destinations.SettingsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.foundation.app.mvi.container

/**
 * Peppol E-Invoicing settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun PeppolSettingsScreen(
    container: PeppolSettingsContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is PeppolSettingsAction.NavigateToPeppolConnect -> {
                navController.navigate(
                    SettingsDestination.PeppolConfiguration.Connect(action.provider.name)
                )
            }
            PeppolSettingsAction.NavigateBack -> navController.navigateUp()
            PeppolSettingsAction.ShowDeleteConfirmation -> {
                // TODO: Show confirmation dialog
            }
            PeppolSettingsAction.ShowDeleteSuccess -> {
                // TODO: Show success message/snackbar
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(PeppolSettingsIntent.LoadSettings)
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.peppol_settings_title)
            )
        }
    ) { contentPadding ->
        with(container.store) {
            PeppolSettingsContent(
                state = state,
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}

/**
 * Peppol settings content without scaffold.
 * Simplified version - shows connection status and connect/disconnect buttons.
 * The actual credentials entry is handled in PeppolConnectScreen.
 */
@Composable
fun IntentReceiver<PeppolSettingsIntent>.PeppolSettingsContent(
    state: PeppolSettingsState,
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
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
        is PeppolSettingsState.Connected -> {
            SettingsContent(
                isConnected = true,
                connectedCompany = state.connectedCompany,
                isDeleting = false,
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
        is PeppolSettingsState.Deleting -> {
            SettingsContent(
                isConnected = true,
                connectedCompany = null,
                isDeleting = true,
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
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun IntentReceiver<PeppolSettingsIntent>.SettingsContent(
    isConnected: Boolean,
    connectedCompany: RecommandCompanySummary?,
    isDeleting: Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Connection Status Card
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
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
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                            intent(PeppolSettingsIntent.SelectProvider(PeppolProvider.Recommand))
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
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        onClick = { intent(PeppolSettingsIntent.DeleteSettingsClicked) },
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
    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(width = 1.dp, color = borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
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
