package tech.dokus.features.cashflow.presentation.peppol.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
import tech.dokus.foundation.aura.components.common.DokusErrorContent

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
        ) {
            when (state) {
                is PeppolRegistrationState.Loading -> LoadingContent()
                is PeppolRegistrationState.Welcome -> WelcomeContent(state, onIntent)
                is PeppolRegistrationState.VerificationResult -> VerificationResultContent(state, onIntent)
                is PeppolRegistrationState.Active -> ActiveContent(state.registration, onIntent)
                is PeppolRegistrationState.WaitingTransfer -> WaitingTransferContent(state, onIntent)
                is PeppolRegistrationState.SendingOnly -> SendingOnlyContent(state.registration, onIntent)
                is PeppolRegistrationState.External -> ExternalContent(state.registration, onIntent)
                is PeppolRegistrationState.Pending -> PendingContent(state.registration, onIntent)
                is PeppolRegistrationState.Failed -> FailedContent(state.registration, onIntent)
                is PeppolRegistrationState.Error -> DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun WelcomeContent(
    state: PeppolRegistrationState.Welcome,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Enable PEPPOL E-Invoicing",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connect your business to the PEPPOL network to send and receive e-invoices automatically.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Enter your enterprise number",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.enterpriseNumber,
                    onValueChange = { onIntent(PeppolRegistrationIntent.UpdateEnterpriseNumber(it)) },
                    label = { Text("Enterprise Number (OGM)") },
                    placeholder = { Text("0123456789") },
                    isError = state.verificationError != null,
                    supportingText = state.verificationError?.let { { Text(it) } },
                    enabled = !state.isVerifying,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onIntent(PeppolRegistrationIntent.VerifyPeppolId) },
                    enabled = state.enterpriseNumber.isNotBlank() && !state.isVerifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (state.isVerifying) "Verifying..." else "Check Availability")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onIntent(PeppolRegistrationIntent.SkipSetup) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'll do this later")
        }
    }
}

@Composable
private fun VerificationResultContent(
    state: PeppolRegistrationState.VerificationResult,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        if (state.result.canProceed) {
            // ID is available
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PEPPOL ID Available",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your PEPPOL ID (${state.result.peppolId}) is available for registration.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onIntent(PeppolRegistrationIntent.EnablePeppol) },
                enabled = !state.isEnabling,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isEnabling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (state.isEnabling) "Enabling..." else "Enable PEPPOL")
            }
        } else {
            // ID is blocked
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PEPPOL ID Already Registered",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your PEPPOL ID (${state.result.peppolId}) is currently registered with ${state.result.blockedBy ?: "another provider"}.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What would you like to do?",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { onIntent(PeppolRegistrationIntent.WaitForTransfer) },
                        enabled = !state.isEnabling,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Wait for Transfer")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose this if you plan to transfer your PEPPOL registration from your current provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { onIntent(PeppolRegistrationIntent.OptOut) },
                        enabled = !state.isEnabling,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Externally")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Choose this if you prefer to keep your current PEPPOL provider.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onIntent(PeppolRegistrationIntent.BackToWelcome) },
            enabled = !state.isEnabling
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ActiveContent(
    registration: PeppolRegistrationDto,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusContentInner(
            icon = Icons.Default.CheckCircle,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "PEPPOL Connected",
            subtitle = "Your business is connected to the PEPPOL network.",
            registration = registration,
            capabilities = listOf(
                "Can receive invoices" to registration.canReceive,
                "Can send invoices" to registration.canSend
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.GoToApp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to App")
        }
    }
}

@Composable
private fun WaitingTransferContent(
    state: PeppolRegistrationState.WaitingTransfer,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Waiting for Transfer",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We're waiting for your PEPPOL registration to be transferred from your current provider.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        RegistrationInfoCard(state.registration)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.PollTransfer) },
            enabled = !state.isPolling,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isPolling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (state.isPolling) "Checking..." else "Check Status")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onIntent(PeppolRegistrationIntent.GoToApp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'll check back later")
        }
    }
}

@Composable
private fun SendingOnlyContent(
    registration: PeppolRegistrationDto,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusContentInner(
            icon = Icons.Default.Send,
            iconTint = MaterialTheme.colorScheme.tertiary,
            title = "Sending Only",
            subtitle = "You can send invoices via PEPPOL, but receiving is managed by another provider.",
            registration = registration,
            capabilities = listOf(
                "Can receive invoices" to registration.canReceive,
                "Can send invoices" to registration.canSend
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.GoToApp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to App")
        }
    }
}

@Composable
private fun ExternalContent(
    registration: PeppolRegistrationDto,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusContentInner(
            icon = Icons.Default.Info,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = "Managed Externally",
            subtitle = "Your PEPPOL registration is managed by another provider.",
            registration = registration
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.GoToApp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Go to App")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onIntent(PeppolRegistrationIntent.BackToWelcome) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set up Peppol")
        }
    }
}

@Composable
private fun PendingContent(
    registration: PeppolRegistrationDto,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusContentInner(
            icon = Icons.Default.HourglassEmpty,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "Registration Pending",
            subtitle = "Your PEPPOL registration is being processed. This usually takes a few minutes.",
            registration = registration
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.GoToApp) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I'll check back later")
        }
    }
}

@Composable
private fun FailedContent(
    registration: PeppolRegistrationDto,
    onIntent: (PeppolRegistrationIntent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Registration Failed",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = registration.errorMessage ?: "An error occurred during PEPPOL registration.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onIntent(PeppolRegistrationIntent.BackToWelcome) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { onIntent(PeppolRegistrationIntent.SkipSetup) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}

@Composable
private fun StatusContentInner(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    registration: PeppolRegistrationDto,
    capabilities: List<Pair<String, Boolean>> = emptyList()
) {
    Spacer(modifier = Modifier.height(48.dp))

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(64.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    RegistrationInfoCard(registration)

    if (capabilities.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Capabilities",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                capabilities.forEach { (label, enabled) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label)
                        Icon(
                            imageVector = if (enabled) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegistrationInfoCard(registration: PeppolRegistrationDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PEPPOL ID",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = registration.peppolId,
                style = MaterialTheme.typography.bodyLarge
            )

            if (registration.testMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Test Mode",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
