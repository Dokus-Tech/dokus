package tech.dokus.features.cashflow.presentation.peppol.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_reg_activating_body
import tech.dokus.aura.resources.peppol_reg_activating_subtitle
import tech.dokus.aura.resources.peppol_reg_activating_title
import tech.dokus.aura.resources.peppol_reg_active_subtitle
import tech.dokus.aura.resources.peppol_reg_active_title
import tech.dokus.aura.resources.peppol_reg_blocked_body
import tech.dokus.aura.resources.peppol_reg_blocked_subtitle
import tech.dokus.aura.resources.peppol_reg_blocked_title
import tech.dokus.aura.resources.peppol_reg_continue
import tech.dokus.aura.resources.peppol_reg_details
import tech.dokus.aura.resources.peppol_reg_enable_later
import tech.dokus.aura.resources.peppol_reg_enable_sending_only
import tech.dokus.aura.resources.peppol_reg_external_subtitle
import tech.dokus.aura.resources.peppol_reg_external_title
import tech.dokus.aura.resources.peppol_reg_failed_footnote
import tech.dokus.aura.resources.peppol_reg_failed_retry
import tech.dokus.aura.resources.peppol_reg_failed_skip
import tech.dokus.aura.resources.peppol_reg_failed_subtitle
import tech.dokus.aura.resources.peppol_reg_failed_title
import tech.dokus.aura.resources.peppol_reg_fresh_enable
import tech.dokus.aura.resources.peppol_reg_fresh_enabling
import tech.dokus.aura.resources.peppol_reg_fresh_subtitle
import tech.dokus.aura.resources.peppol_reg_fresh_title
import tech.dokus.aura.resources.peppol_reg_not_now
import tech.dokus.aura.resources.peppol_reg_peppol_id
import tech.dokus.aura.resources.peppol_reg_sending_subtitle
import tech.dokus.aura.resources.peppol_reg_sending_title
import tech.dokus.aura.resources.peppol_reg_transfer_inbox
import tech.dokus.aura.resources.peppol_reg_transfer_request
import tech.dokus.aura.resources.peppol_reg_waiting_body
import tech.dokus.aura.resources.peppol_reg_waiting_subtitle
import tech.dokus.aura.resources.peppol_reg_waiting_title
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.AnimatedCheck
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.PCopyRow
import tech.dokus.foundation.aura.components.common.WaitingIndicator
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun PeppolRegistrationScreen(
    state: PeppolRegistrationState,
    snackbarHostState: SnackbarHostState,
    onIntent: (PeppolRegistrationIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .limitWidthCenteredContent(),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                PeppolRegistrationState.Loading -> LoadingContent()
                is PeppolRegistrationState.Fresh -> FreshContent(state, onIntent)
                is PeppolRegistrationState.Activating -> ActivatingContent(onIntent)
                is PeppolRegistrationState.Active -> ActiveContent(state, onIntent)
                is PeppolRegistrationState.Blocked -> BlockedContent(state, onIntent)
                is PeppolRegistrationState.WaitingTransfer -> WaitingTransferContent(
                    state,
                    onIntent
                )

                is PeppolRegistrationState.SendingOnly -> SendingOnlyContent(state, onIntent)
                is PeppolRegistrationState.External -> ExternalContent(onIntent)
                is PeppolRegistrationState.Failed -> FailedContent(state, onIntent)
                is PeppolRegistrationState.Error -> DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler,
                    modifier = Modifier.fillMaxWidth().padding(Constrains.Spacing.large)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    CircularProgressIndicator()
}

@Composable
private fun FreshContent(
    state: PeppolRegistrationState.Fresh,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = { PeppolCircle() },
        title = "Enable Peppol",
        subtitle = "Receive invoices automatically in Dokus.",
        primary = {
            POutlinedButton(
                text = if (state.isEnabling) "Enabling…" else "Enable",
                enabled = !state.isEnabling,
                isLoading = state.isEnabling,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(PeppolRegistrationIntent.EnablePeppol) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.NotNow) }) {
                Text(
                    text = "Not now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        footnote = "You can enable this later in Settings.",
    )
}

@Composable
private fun ActivatingContent(
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = { PeppolSpinner() },
        title = "Enabling Peppol",
        subtitle = "Connecting your inbox.",
        primary = {
            POutlinedButton(
                text = "Continue",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        body = {
            Text(
                text = "This can take a moment.\nYou can continue working.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    )
}

@Composable
private fun ActiveContent(
    state: PeppolRegistrationState.Active,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { AnimatedCheck(play = true) },
        title = "Peppol active",
        subtitle = "Inbox connected. Sending enabled.",
        primary = {
            POutlinedButton(
                text = "Continue",
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = "Details",
                isExpanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded }
            ) {
                PCopyRow(label = "Peppol ID", value = state.context.peppolId)
            }
        }
    )
}

@Composable
private fun BlockedContent(
    state: PeppolRegistrationState.Blocked,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var transferExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { PeppolCircle() },
        title = "Peppol already connected",
        subtitle = "Your inbox is connected to another service.",
        body = {
            Text(
                text = "To receive invoices in Dokus, request a transfer from your current provider.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Constrains.Spacing.medium)
            )
        },
        primary = {
            POutlinedButton(
                text = "Transfer inbox to Dokus",
                enabled = !state.isWorking,
                isLoading = state.isWorking,
                onClick = { onIntent(PeppolRegistrationIntent.WaitForTransfer) }
            )
        },
        secondary = {
            TextButton(
                onClick = { onIntent(PeppolRegistrationIntent.EnableSendingOnly) },
                enabled = !state.isWorking
            ) {
                Text(
                    text = "Enable sending only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        details = {
            PCollapsibleSection(
                title = "Transfer request",
                isExpanded = transferExpanded,
                onToggle = { transferExpanded = !transferExpanded }
            ) {
                TransferEmailCard(
                    companyName = state.context.companyName,
                    peppolId = state.context.peppolId,
                )
            }
        }
    )
}

@Composable
private fun WaitingTransferContent(
    state: PeppolRegistrationState.WaitingTransfer,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var transferExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { WaitingIndicator() },
        title = "Waiting for transfer",
        subtitle = "We'll connect automatically once your inbox is released.",
        body = {
            Text(
                text = "This can take up to 48 hours.\nYou can close this screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Constrains.Spacing.large)
            )
        },
        primary = {
            POutlinedButton(
                text = "Continue",
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = "Transfer request",
                isExpanded = transferExpanded,
                onToggle = { transferExpanded = !transferExpanded }
            ) {
                TransferEmailCard(
                    companyName = state.context.companyName,
                    peppolId = state.context.peppolId,
                )
            }
        }
    )
}

@Composable
private fun SendingOnlyContent(
    state: PeppolRegistrationState.SendingOnly,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { AnimatedCheck(play = true) },
        title = "Peppol sending enabled",
        subtitle = "You can send invoices via Peppol.\nIncoming invoices go to your other service.",
        primary = {
            POutlinedButton(
                text = "Continue",
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = "Details",
                isExpanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded }
            ) {
                PCopyRow(label = "Peppol ID", value = state.context.peppolId)
            }
        }
    )
}

@Composable
private fun ExternalContent(
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = {
            PeppolCircle {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.textMuted)
                )
            }
        },
        title = "Peppol not enabled",
        subtitle = "Dokus won't send or receive Peppol invoices.",
        primary = {
            POutlinedButton(
                text = "Continue",
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.EnablePeppol) }) {
                Text(
                    text = "Enable Peppol",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    )
}

@Composable
private fun FailedContent(
    state: PeppolRegistrationState.Failed,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = {
            PeppolCircle {
                PeppolCloseIcon()
            }
        },
        title = "Couldn't enable Peppol",
        subtitle = "Something went wrong while connecting.",
        primary = {
            POutlinedButton(
                text = "Try again",
                isLoading = state.isRetrying,
                enabled = !state.isRetrying,
                onClick = { onIntent(PeppolRegistrationIntent.Retry) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.NotNow) }) {
                Text(
                    text = "Skip for now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        footnote = "If this keeps happening, contact support.",
    )
}
