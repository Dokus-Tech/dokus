package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.CashflowCard
import ai.dokus.app.cashflow.components.InvoiceCard
import ai.dokus.app.cashflow.components.toCashflowItems
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CashflowScreen(
    viewModel: CashflowViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            PTopAppBar(title = "Cashflow")
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Invoices") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Expenses") }
                )
            }

            // Content
            when (val currentState = state) {
                is CashflowViewModel.State.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is CashflowViewModel.State.Success -> {
                    if (selectedTabIndex == 0) {
                        // Invoices tab
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            // Add the Cashflow Card at the top
                            item {
                                // Convert invoices to CashflowItemData with "Need confirmation" status
                                val cashflowItems = currentState.invoices.toCashflowItems(
                                    limit = 4,
                                    customStatusText = "Need confirmation"
                                )

                                if (cashflowItems.isNotEmpty()) {
                                    CashflowCard(
                                        items = cashflowItems,
                                        onPreviousClick = { /* TODO: Handle previous page */ },
                                        onNextClick = { /* TODO: Handle next page */ },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // Show all invoices below the card
                            if (currentState.invoices.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No invoices yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(currentState.invoices) { invoice ->
                                    InvoiceCard(
                                        invoice = invoice,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Expenses tab
                        if (currentState.expenses.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No expenses yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                items(currentState.expenses) { expense ->
                                    Text(
                                        text = "${expense.merchant} - ${expense.amount}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                is CashflowViewModel.State.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = when (currentState.exception) {
                                    is DokusException.ConnectionError -> "Connection error. Please check your internet connection."
                                    is DokusException.NotAuthenticated -> "Authentication failed. Please log in again."
                                    else -> "An error occurred: ${currentState.exception.message}"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
