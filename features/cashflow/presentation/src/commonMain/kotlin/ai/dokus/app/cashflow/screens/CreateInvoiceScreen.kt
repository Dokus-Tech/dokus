package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.invoice.InvoiceFormCard
import ai.dokus.app.cashflow.components.invoice.InvoiceSummaryCard
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceViewModel
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for creating a new invoice.
 *
 * Desktop: Two-column layout with description on left, form on right.
 * Mobile: Single column with description then form.
 */
@Composable
internal fun CreateInvoiceScreen(
    viewModel: CreateInvoiceViewModel = koinViewModel(),
    onInvoiceCreated: () -> Unit = {}
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    val formState by viewModel.formState.collectAsState()
    val clientsState by viewModel.clientsState.collectAsState()
    val saveState by viewModel.state.collectAsState()
    val createdInvoiceId by viewModel.createdInvoiceId.collectAsState()

    // Navigate back when invoice is created
    LaunchedEffect(createdInvoiceId) {
        if (createdInvoiceId != null) {
            onInvoiceCreated()
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        if (isLargeScreen) {
            DesktopLayout(
                contentPadding = contentPadding,
                descriptionContent = {
                    Text(
                        text = "Create a new invoice",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Fill in the details to create an invoice for your client. You can save it as a draft or send it directly via Peppol.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    InvoiceSummaryCard(formState = formState)
                },
                formContent = {
                    InvoiceFormCard(
                        formState = formState,
                        clientsState = clientsState,
                        saveState = saveState,
                        onSelectClient = viewModel::selectClient,
                        onUpdateNotes = viewModel::updateNotes,
                        onAddLineItem = viewModel::addLineItem,
                        onRemoveLineItem = viewModel::removeLineItem,
                        onUpdateItemDescription = viewModel::updateItemDescription,
                        onUpdateItemQuantity = viewModel::updateItemQuantity,
                        onUpdateItemUnitPrice = viewModel::updateItemUnitPrice,
                        onUpdateItemVatRate = viewModel::updateItemVatRate,
                        onSaveAsDraft = viewModel::saveAsDraft
                    )
                }
            )
        } else {
            MobileLayout(
                contentPadding = contentPadding,
                content = {
                    Text(
                        text = "Create a new invoice",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Fill in the details to create an invoice for your client.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    InvoiceFormCard(
                        formState = formState,
                        clientsState = clientsState,
                        saveState = saveState,
                        onSelectClient = viewModel::selectClient,
                        onUpdateNotes = viewModel::updateNotes,
                        onAddLineItem = viewModel::addLineItem,
                        onRemoveLineItem = viewModel::removeLineItem,
                        onUpdateItemDescription = viewModel::updateItemDescription,
                        onUpdateItemQuantity = viewModel::updateItemQuantity,
                        onUpdateItemUnitPrice = viewModel::updateItemUnitPrice,
                        onUpdateItemVatRate = viewModel::updateItemVatRate,
                        onSaveAsDraft = viewModel::saveAsDraft
                    )

                    InvoiceSummaryCard(formState = formState)

                    Spacer(modifier = Modifier.height(24.dp))
                }
            )
        }
    }
}

@Composable
private fun DesktopLayout(
    contentPadding: PaddingValues,
    descriptionContent: @Composable () -> Unit,
    formContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Left column: Description
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            descriptionContent()
        }

        // Right column: Form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            formContent()
        }
    }
}

@Composable
private fun MobileLayout(
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        content()
    }
}
