package tech.dokus.features.cashflow.presentation.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.StatusBadge
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.features.cashflow.presentation.screenshot.ScreenshotTestWrapper

/**
 * Screenshot tests for cashflow screens.
 * Tests simplified versions of screens to capture UI layouts.
 */
class CashflowScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 600,
            screenHeight = 960,
            density = Density.XXHIGH,
            softButtons = false
        ),
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun cashflowLedgerScreen_empty() {
        paparazzi.snapshot("CashflowLedgerScreen_empty_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                CashflowLedgerContent(entries = emptyList())
            }
        }
        paparazzi.snapshot("CashflowLedgerScreen_empty_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                CashflowLedgerContent(entries = emptyList())
            }
        }
    }

    @Test
    fun cashflowLedgerScreen_withEntries() {
        val sampleEntries = listOf(
            CashflowEntry("Invoice #001", "Acme Corp", "1,500.00", "Paid", Color(0xFF4CAF50)),
            CashflowEntry("Invoice #002", "TechStart Ltd", "2,300.00", "Pending", Color(0xFFFFC107)),
            CashflowEntry("Invoice #003", "Global Inc", "850.00", "Overdue", Color(0xFFF44336)),
            CashflowEntry("Invoice #004", "Local Shop", "450.00", "Draft", Color(0xFF9E9E9E))
        )

        paparazzi.snapshot("CashflowLedgerScreen_withEntries_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                CashflowLedgerContent(entries = sampleEntries)
            }
        }
        paparazzi.snapshot("CashflowLedgerScreen_withEntries_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                CashflowLedgerContent(entries = sampleEntries)
            }
        }
    }

    @Test
    fun documentsScreen() {
        val sampleDocs = listOf(
            DocumentEntry("Contract_2024.pdf", "2024-01-15", "Processed"),
            DocumentEntry("Invoice_001.pdf", "2024-01-10", "Processing"),
            DocumentEntry("Receipt_shop.jpg", "2024-01-05", "Pending")
        )

        paparazzi.snapshot("DocumentsScreen_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                DocumentsContent(documents = sampleDocs)
            }
        }
        paparazzi.snapshot("DocumentsScreen_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                DocumentsContent(documents = sampleDocs)
            }
        }
    }

    @Test
    fun createInvoiceScreen() {
        paparazzi.snapshot("CreateInvoiceScreen_light") {
            ScreenshotTestWrapper(isDarkMode = false) {
                CreateInvoiceContent()
            }
        }
        paparazzi.snapshot("CreateInvoiceScreen_dark") {
            ScreenshotTestWrapper(isDarkMode = true) {
                CreateInvoiceContent()
            }
        }
    }
}

private data class CashflowEntry(
    val title: String,
    val subtitle: String,
    val amount: String,
    val status: String,
    val statusColor: Color
)

private data class DocumentEntry(
    val name: String,
    val date: String,
    val status: String
)

@Composable
private fun CashflowLedgerContent(entries: List<CashflowEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "Cashflow",
            navController = null,
            showBackButton = false
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = true, onClick = {}, label = { Text("All") })
            FilterChip(selected = false, onClick = {}, label = { Text("Income") })
            FilterChip(selected = false, onClick = {}, label = { Text("Expense") })
        }

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No entries yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    POutlinedButton(
                        text = "Create Invoice",
                        onClick = {}
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.forEach { entry ->
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = entry.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "€${entry.amount}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                StatusBadge(
                                    text = entry.status,
                                    color = entry.statusColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentsContent(documents: List<DocumentEntry>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "Documents",
            navController = null,
            showBackButton = false
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${documents.size} documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            POutlinedButton(
                text = "Upload",
                onClick = {}
            )
        }

        DokusCard(modifier = Modifier.padding(16.dp)) {
            Column {
                documents.forEachIndexed { index, doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = doc.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = doc.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = doc.status,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < documents.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateInvoiceContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PTopAppBar(
            title = "New Invoice",
            navController = null,
            showBackButton = true
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Client",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select a client...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Line Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        POutlinedButton(text = "Add Item", onClick = {})
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No items added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text("€0.00", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("VAT (21%)", style = MaterialTheme.typography.bodyMedium)
                        Text("€0.00", style = MaterialTheme.typography.bodyMedium)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "€0.00",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
