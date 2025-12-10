package ai.dokus.app.screens.settings

import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.save_changes
import ai.dokus.app.resources.generated.workspace_address
import ai.dokus.app.resources.generated.workspace_banking
import ai.dokus.app.resources.generated.workspace_bic
import ai.dokus.app.resources.generated.workspace_company_info
import ai.dokus.app.resources.generated.workspace_company_name
import ai.dokus.app.resources.generated.workspace_iban
import ai.dokus.app.resources.generated.workspace_invoice_prefix
import ai.dokus.app.resources.generated.workspace_invoice_settings
import ai.dokus.app.resources.generated.workspace_legal_name
import ai.dokus.app.resources.generated.workspace_payment_terms
import ai.dokus.app.resources.generated.workspace_settings_title
import ai.dokus.app.resources.generated.workspace_vat_number
import ai.dokus.app.viewmodel.SaveState
import ai.dokus.app.viewmodel.WorkspaceSettingsViewModel
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Workspace/Company settings screen.
 * Allows users to configure company info, banking details, and invoice settings.
 */
@Composable
fun WorkspaceSettingsScreen(
    viewModel: WorkspaceSettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadWorkspaceSettings()
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.workspace_settings_title)
            )
        }
    ) { contentPadding ->
        when {
            state.isLoading() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.isSuccess() -> {
                Column(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .withContentPaddingForScrollable(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Company Information Section
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.workspace_company_info),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(16.dp))

                            // Legal Name (read-only)
                            Text(
                                text = stringResource(Res.string.workspace_legal_name),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formState.legalName,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_company_name),
                                value = formState.companyName,
                                onValueChange = { viewModel.updateCompanyName(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_vat_number),
                                value = formState.vatNumber,
                                onValueChange = { viewModel.updateVatNumber(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_address),
                                value = formState.address,
                                onValueChange = { viewModel.updateAddress(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Banking Details Section
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.workspace_banking),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(16.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_iban),
                                value = formState.iban,
                                onValueChange = { viewModel.updateIban(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_bic),
                                value = formState.bic,
                                onValueChange = { viewModel.updateBic(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Invoice Settings Section
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(Res.string.workspace_invoice_settings),
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(16.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_invoice_prefix),
                                value = formState.invoicePrefix,
                                onValueChange = { viewModel.updateInvoicePrefix(it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(12.dp))

                            PTextFieldStandard(
                                fieldName = stringResource(Res.string.workspace_payment_terms),
                                value = formState.defaultPaymentTerms.toString(),
                                onValueChange = { viewModel.updateDefaultPaymentTerms(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Save Button
                    PPrimaryButton(
                        text = stringResource(Res.string.save_changes),
                        enabled = saveState !is SaveState.Saving,
                        onClick = { viewModel.saveWorkspaceSettings() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save State Feedback
                    when (saveState) {
                        is SaveState.Success -> {
                            Text(
                                text = "Settings saved successfully",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        is SaveState.Error -> {
                            Text(
                                text = (saveState as SaveState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                        else -> {}
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
