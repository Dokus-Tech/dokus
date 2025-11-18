package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.FinancialDocumentTable
import ai.dokus.app.cashflow.components.VatSummaryCard
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.components.combineFinancialDocuments
import ai.dokus.app.cashflow.components.financialDocumentsNeedingConfirmation
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.foundation.design.components.common.Breakpoints
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.FinancialDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main cashflow screen showing financial documents table and VAT summary.
 * Responsive layout that adapts to mobile and desktop screen sizes.
 */
@Composable
internal fun CashflowScreen(
    viewModel: CashflowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            PTopAppBar(title = "Cashflow")
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        // Main content based on state
        when (val currentState = state) {
            is CashflowViewModel.State.Loading -> {
                LoadingContent(contentPadding)
            }

            is CashflowViewModel.State.Success -> {
                SuccessContent(
                    invoices = currentState.invoices,
                    expenses = currentState.expenses,
                    contentPadding = contentPadding,
                    onDocumentClick = { document ->
                        // TODO: Navigate to document detail
                    },
                    onMoreClick = { document ->
                        // TODO: Show context menu
                    }
                )
            }

            is CashflowViewModel.State.Error -> {
                ErrorContent(
                    exception = currentState.exception,
                    contentPadding = contentPadding,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

/**
 * Loading state content with centered progress indicator.
 */
@Composable
private fun LoadingContent(
    contentPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Error state content with error message and retry option.
 */
@Composable
private fun ErrorContent(
    exception: DokusException,
    contentPadding: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = when (exception) {
                    is DokusException.ConnectionError -> "Connection error. Please check your internet connection."
                    is DokusException.NotAuthenticated -> "Authentication failed. Please log in again."
                    else -> "An error occurred: ${exception.message}"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Success state content with responsive layout for financial documents and VAT summary.
 * Adapts layout based on screen width.
 */
@Composable
private fun SuccessContent(
    invoices: List<ai.dokus.foundation.domain.model.Invoice>,
    expenses: List<ai.dokus.foundation.domain.model.Expense>,
    contentPadding: PaddingValues,
    onDocumentClick: (FinancialDocument) -> Unit,
    onMoreClick: (FinancialDocument) -> Unit
) {
    // Convert domain models to FinancialDocuments
    val allDocuments = combineFinancialDocuments(
        invoices = invoices,
        expenses = expenses,
        limit = 50 // Show more documents in the table
    )

    // Calculate VAT summary data (placeholder values for now)
    val vatSummaryData = calculateVatSummary(invoices, expenses)

    // Use BoxWithConstraints to determine layout based on screen size
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        val isLargeScreen = maxWidth >= Breakpoints.LARGE.dp

        if (isLargeScreen) {
            // Desktop layout: Two columns with table on left, VAT summary on right
            DesktopLayout(
                documents = allDocuments,
                vatSummaryData = vatSummaryData,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick
            )
        } else {
            // Mobile layout: Single column with scrollable content
            MobileLayout(
                documents = allDocuments,
                vatSummaryData = vatSummaryData,
                onDocumentClick = onDocumentClick,
                onMoreClick = onMoreClick
            )
        }
    }
}

/**
 * Calculates VAT summary from invoices and expenses.
 * TODO: Replace with actual calculations based on business logic.
 */
private fun calculateVatSummary(
    invoices: List<ai.dokus.foundation.domain.model.Invoice>,
    expenses: List<ai.dokus.foundation.domain.model.Expense>
): VatSummaryData {
    // Calculate total VAT from invoices
    val totalVat = invoices.sumOf { it.vatAmount.value.toDoubleOrNull() ?: 0.0 }
    
    // Calculate net amount (total - vat)
    val totalInvoices = invoices.sumOf { it.totalAmount.value.toDoubleOrNull() ?: 0.0 }
    val totalExpenses = expenses.sumOf { it.amount.value.toDoubleOrNull() ?: 0.0 }
    val netAmount = totalInvoices - totalExpenses
    
    // Predicted net amount (simplified prediction)
    val predictedNet = netAmount * 1.1 // 10% growth prediction
    
    return VatSummaryData(
        vatAmount = ai.dokus.foundation.domain.Money(totalVat.toString()),
        netAmount = ai.dokus.foundation.domain.Money(netAmount.toString()),
        predictedNetAmount = ai.dokus.foundation.domain.Money(predictedNet.toString()),
        quarterInfo = null
    )
}

/**
 * Desktop layout with two-column structure.
 * Left: Financial documents table
 * Right: VAT summary card (sticky)
 */
@Composable
private fun DesktopLayout(
    documents: List<FinancialDocument>,
    vatSummaryData: VatSummaryData,
    onDocumentClick: (FinancialDocument) -> Unit,
    onMoreClick: (FinancialDocument) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left column: Financial documents table
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section title
            item {
                Text(
                    text = "Financial Documents",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Documents needing confirmation section
            val pendingDocuments = documents.financialDocumentsNeedingConfirmation()
            if (pendingDocuments.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Needs Confirmation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        FinancialDocumentTable(
                            documents = pendingDocuments,
                            onDocumentClick = onDocumentClick,
                            onMoreClick = onMoreClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // All documents section
            item {
                Text(
                    text = "All Documents",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                if (documents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial documents yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    FinancialDocumentTable(
                        documents = documents,
                        onDocumentClick = onDocumentClick,
                        onMoreClick = onMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Right column: VAT Summary Card (fixed width)
        Box(
            modifier = Modifier.width(360.dp)
        ) {
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Mobile layout with single-column scrollable content.
 * Stacks VAT summary above the documents table.
 */
@Composable
private fun MobileLayout(
    documents: List<FinancialDocument>,
    vatSummaryData: VatSummaryData,
    onDocumentClick: (FinancialDocument) -> Unit,
    onMoreClick: (FinancialDocument) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // VAT Summary Card at top
        item {
            VatSummaryCard(
                vatAmount = vatSummaryData.vatAmount,
                netAmount = vatSummaryData.netAmount,
                predictedNetAmount = vatSummaryData.predictedNetAmount,
                quarterInfo = vatSummaryData.quarterInfo,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Section title
        item {
            Text(
                text = "Financial Documents",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Documents needing confirmation section
        val pendingDocuments = documents.financialDocumentsNeedingConfirmation()
        if (pendingDocuments.isNotEmpty()) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Needs Confirmation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // On mobile, show a more compact list view
                    MobileDocumentList(
                        documents = pendingDocuments,
                        onDocumentClick = onDocumentClick
                    )
                }
            }
        }

        // All documents section
        item {
            Text(
                text = "All Documents",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (documents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No financial documents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(documents) { document ->
                MobileDocumentCard(
                    document = document,
                    onClick = { onDocumentClick(document) }
                )
            }
        }

        // Add bottom padding for navigation bar
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Compact list view for mobile showing document items.
 */
@Composable
private fun MobileDocumentList(
    documents: List<FinancialDocument>,
    onDocumentClick: (FinancialDocument) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        documents.forEach { document ->
            MobileDocumentCard(
                document = document,
                onClick = { onDocumentClick(document) }
            )
        }
    }
}

/**
 * Compact card for mobile showing a single document.
 */
@Composable
private fun MobileDocumentCard(
    document: FinancialDocument,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Document info
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Alert indicator if needed
                    if (document.status == ai.dokus.foundation.domain.model.FinancialDocumentStatus.PendingApproval) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                    }
                    
                    Text(
                        text = document.documentNumber,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Contact/merchant name
                val contactName = when (document) {
                    is FinancialDocument.InvoiceDocument -> "Name Surname" // TODO: Get from client
                    is FinancialDocument.ExpenseDocument -> document.merchant
                }
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Amount and date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "€${document.amount.value}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = document.date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right side: Type badge
            ai.dokus.foundation.design.components.CashflowTypeBadge(
                type = when (document) {
                    is FinancialDocument.InvoiceDocument -> ai.dokus.foundation.design.components.CashflowType.CashIn
                    is FinancialDocument.ExpenseDocument -> ai.dokus.foundation.design.components.CashflowType.CashOut
                }
            )
        }
    }
}
