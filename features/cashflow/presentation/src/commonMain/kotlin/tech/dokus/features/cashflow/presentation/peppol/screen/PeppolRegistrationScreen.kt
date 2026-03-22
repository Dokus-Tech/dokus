package tech.dokus.features.cashflow.presentation.peppol.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationPhase
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolSetupContext
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.common.AnimatedCheck
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.PCopyRow
import tech.dokus.foundation.aura.components.common.WaitingIndicator
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun PeppolRegistrationScreen(
    state: PeppolRegistrationState,
    onIntent: (PeppolRegistrationIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.actionError?.let { error ->
                DokusErrorBanner(
                    exception = error,
                    retryHandler = null,
                    modifier = Modifier.padding(horizontal = Constraints.Spacing.large),
                    onDismiss = { onIntent(PeppolRegistrationIntent.DismissActionError) },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .limitWidthCenteredContent(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.setupContext.isLoading() -> LoadingContent()

                    state.setupContext.isError() -> {
                        val error = state.setupContext.exception
                        if (isPeppolSetupFlowError(error)) {
                            SetupErrorContent(
                                exception = error,
                                retryHandler = state.setupContext.retryHandler,
                                onIntent = onIntent,
                            )
                        } else {
                            DokusErrorContent(
                                exception = error,
                                retryHandler = state.setupContext.retryHandler,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    state.setupContext.isSuccess() -> {
                        val context = state.setupContext.data
                        when (state.phase) {
                            PeppolRegistrationPhase.Fresh -> FreshContent(
                                isEnabling = state.isWorking,
                                onIntent = onIntent,
                            )

                            PeppolRegistrationPhase.Activating -> ActivatingContent(onIntent)

                            PeppolRegistrationPhase.Active -> ActiveContent(
                                context = context,
                                onIntent = onIntent,
                            )

                            PeppolRegistrationPhase.Blocked -> BlockedContent(
                                context = context,
                                isWorking = state.isWorking,
                                onIntent = onIntent,
                            )

                            PeppolRegistrationPhase.WaitingTransfer -> WaitingTransferContent(
                                context = context,
                                onIntent = onIntent,
                            )

                            PeppolRegistrationPhase.SendingOnly -> SendingOnlyContent(
                                context = context,
                                onIntent = onIntent,
                            )

                            PeppolRegistrationPhase.External -> ExternalContent(onIntent)

                            PeppolRegistrationPhase.Failed -> FailedContent(
                                isRetrying = state.isRetrying,
                                onIntent = onIntent,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    DokusLoader()
}

@Composable
private fun FreshContent(
    isEnabling: Boolean,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = { PeppolCircle() },
        title = stringResource(Res.string.peppol_reg_fresh_title),
        subtitle = stringResource(Res.string.peppol_reg_fresh_subtitle),
        primary = {
            POutlinedButton(
                text = if (isEnabling) {
                    stringResource(Res.string.peppol_reg_fresh_enabling)
                } else {
                    stringResource(Res.string.peppol_reg_fresh_enable)
                },
                enabled = !isEnabling,
                isLoading = isEnabling,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(PeppolRegistrationIntent.EnablePeppol) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.NotNow) }) {
                Text(
                    text = stringResource(Res.string.peppol_reg_not_now),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        footnote = stringResource(Res.string.peppol_reg_enable_later),
    )
}

@Composable
private fun ActivatingContent(
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = { PeppolSpinner() },
        title = stringResource(Res.string.peppol_reg_activating_title),
        subtitle = stringResource(Res.string.peppol_reg_activating_subtitle),
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_continue),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        body = {
            Text(
                text = stringResource(Res.string.peppol_reg_activating_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    )
}

@Composable
private fun ActiveContent(
    context: PeppolSetupContext,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { AnimatedCheck(play = true) },
        title = stringResource(Res.string.peppol_reg_active_title),
        subtitle = stringResource(Res.string.peppol_reg_active_subtitle),
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_continue),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = stringResource(Res.string.peppol_reg_details),
                isExpanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded }
            ) {
                PCopyRow(label = stringResource(Res.string.peppol_reg_peppol_id), value = context.peppolId)
            }
        }
    )
}

@Composable
private fun BlockedContent(
    context: PeppolSetupContext,
    isWorking: Boolean,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var transferExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { PeppolCircle() },
        title = stringResource(Res.string.peppol_reg_blocked_title),
        subtitle = stringResource(Res.string.peppol_reg_blocked_subtitle),
        body = {
            Text(
                text = stringResource(Res.string.peppol_reg_blocked_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Constraints.Spacing.medium)
            )
        },
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_transfer_inbox),
                enabled = !isWorking,
                isLoading = isWorking,
                onClick = { onIntent(PeppolRegistrationIntent.WaitForTransfer) }
            )
        },
        secondary = {
            TextButton(
                onClick = { onIntent(PeppolRegistrationIntent.EnableSendingOnly) },
                enabled = !isWorking
            ) {
                Text(
                    text = stringResource(Res.string.peppol_reg_enable_sending_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        details = {
            PCollapsibleSection(
                title = stringResource(Res.string.peppol_reg_transfer_request),
                isExpanded = transferExpanded,
                onToggle = { transferExpanded = !transferExpanded }
            ) {
                TransferEmailCard(
                    companyName = context.companyName,
                    peppolId = context.peppolId,
                )
            }
        }
    )
}

@Composable
private fun WaitingTransferContent(
    context: PeppolSetupContext,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var transferExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { WaitingIndicator() },
        title = stringResource(Res.string.peppol_reg_waiting_title),
        subtitle = stringResource(Res.string.peppol_reg_waiting_subtitle),
        body = {
            Text(
                text = stringResource(Res.string.peppol_reg_waiting_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Constraints.Spacing.large)
            )
        },
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_continue),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = stringResource(Res.string.peppol_reg_transfer_request),
                isExpanded = transferExpanded,
                onToggle = { transferExpanded = !transferExpanded }
            ) {
                TransferEmailCard(
                    companyName = context.companyName,
                    peppolId = context.peppolId,
                )
            }
        }
    )
}

@Composable
private fun SendingOnlyContent(
    context: PeppolSetupContext,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    var detailsExpanded by remember { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = { AnimatedCheck(play = true) },
        title = stringResource(Res.string.peppol_reg_sending_title),
        subtitle = stringResource(Res.string.peppol_reg_sending_subtitle),
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_continue),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        details = {
            PCollapsibleSection(
                title = stringResource(Res.string.peppol_reg_details),
                isExpanded = detailsExpanded,
                onToggle = { detailsExpanded = !detailsExpanded }
            ) {
                PCopyRow(label = stringResource(Res.string.peppol_reg_peppol_id), value = context.peppolId)
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
        title = stringResource(Res.string.peppol_reg_external_title),
        subtitle = stringResource(Res.string.peppol_reg_external_subtitle),
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_continue),
                onClick = { onIntent(PeppolRegistrationIntent.Continue) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.EnablePeppol) }) {
                Text(
                    text = stringResource(Res.string.peppol_reg_fresh_title),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    )
}

@Composable
private fun FailedContent(
    isRetrying: Boolean,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    PeppolCenteredFlow(
        icon = {
            PeppolCircle {
                PeppolCloseIcon()
            }
        },
        title = stringResource(Res.string.peppol_reg_failed_title),
        subtitle = stringResource(Res.string.peppol_reg_failed_subtitle),
        primary = {
            POutlinedButton(
                text = stringResource(Res.string.peppol_reg_failed_retry),
                isLoading = isRetrying,
                enabled = !isRetrying,
                onClick = { onIntent(PeppolRegistrationIntent.Retry) }
            )
        },
        secondary = {
            TextButton(onClick = { onIntent(PeppolRegistrationIntent.NotNow) }) {
                Text(
                    text = stringResource(Res.string.peppol_reg_failed_skip),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        },
        footnote = stringResource(Res.string.peppol_reg_failed_footnote),
    )
}

@Composable
private fun SetupErrorContent(
    exception: DokusException,
    retryHandler: RetryHandler,
    onIntent: (PeppolRegistrationIntent) -> Unit,
) {
    val canRetry = shouldShowPeppolSetupRetry(exception)
    var retryClicked by remember(exception) { mutableStateOf(false) }

    PeppolCenteredFlow(
        icon = {
            PeppolCircle {
                PeppolCloseIcon()
            }
        },
        title = stringResource(Res.string.peppol_reg_failed_title),
        subtitle = exception.localized,
        primary = {
            if (canRetry) {
                POutlinedButton(
                    text = stringResource(Res.string.state_retry),
                    isLoading = retryClicked,
                    enabled = !retryClicked,
                    onClick = {
                        retryClicked = true
                        retryHandler.retry()
                    }
                )
            } else {
                POutlinedButton(
                    text = stringResource(Res.string.peppol_reg_failed_skip),
                    onClick = { onIntent(PeppolRegistrationIntent.NotNow) }
                )
            }
        },
        secondary = {
            if (canRetry) {
                TextButton(onClick = { onIntent(PeppolRegistrationIntent.NotNow) }) {
                    Text(
                        text = stringResource(Res.string.peppol_reg_failed_skip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
        },
        footnote = stringResource(Res.string.peppol_reg_enable_later),
    )
}

internal fun isPeppolSetupFlowError(exception: DokusException): Boolean =
    exception is DokusException.PeppolDirectoryUnavailable ||
        exception is DokusException.ConnectionError

internal fun shouldShowPeppolSetupRetry(exception: DokusException): Boolean =
    isPeppolSetupFlowError(exception) && exception.recoverable

@Preview
@Composable
private fun PeppolRegistrationScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PeppolRegistrationScreen(
            state = PeppolRegistrationState(),
            onIntent = {},
        )
    }
}
