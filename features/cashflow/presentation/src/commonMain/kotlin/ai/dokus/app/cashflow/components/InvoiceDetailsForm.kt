package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import ai.dokus.foundation.design.components.CashflowType
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Form a component for invoice details.
 * Displays fields for invoice name, amount, VAT, dates, client, etc.
 */
@Composable
fun InvoiceDetailsForm(
    modifier: Modifier = Modifier
) {
    var invoiceName by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("") }
    var vat by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var client by remember { mutableStateOf("") }
    var isRecurrent by remember { mutableStateOf(false) }
    var cashflowType by remember { mutableStateOf(CashflowType.CashIn) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(Res.string.invoice_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Invoice name
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_name),
                value = invoiceName,
                onValueChange = { invoiceName = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Total amount
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_total_amount),
                value = totalAmount,
                onValueChange = { totalAmount = it },
                modifier = Modifier.fillMaxWidth()
            )

            // VAT
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_vat),
                value = vat,
                onValueChange = { vat = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Category
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_category),
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Issue date
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_issue_date),
                value = issueDate,
                onValueChange = { issueDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Due date
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_due_date),
                value = dueDate,
                onValueChange = { dueDate = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Client
            PTextFieldStandard(
                fieldName = stringResource(Res.string.invoice_client),
                value = client,
                onValueChange = { client = it },
                modifier = Modifier.fillMaxWidth()
            )

            // Recurrent expense
            FormField(label = stringResource(Res.string.invoice_recurrent_expense)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isRecurrent,
                            onClick = { isRecurrent = true }
                        )
                        Text(
                            text = stringResource(Res.string.answer_yes),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isRecurrent,
                            onClick = { isRecurrent = false }
                        )
                        Text(
                            text = stringResource(Res.string.answer_no),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Cashflow
            FormField(label = stringResource(Res.string.cashflow_direction)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cashflowType == CashflowType.CashIn,
                            onClick = { cashflowType = CashflowType.CashIn }
                        )
                        Text(
                            text = stringResource(Res.string.cashflow_cash_in),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = cashflowType == CashflowType.CashOut,
                            onClick = { cashflowType = CashflowType.CashOut }
                        )
                        Text(
                            text = stringResource(Res.string.cashflow_cash_out),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Items
            FormField(label = stringResource(Res.string.invoice_items)) {
                Text(
                    text = stringResource(Res.string.invoice_no_items),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Form field with label and content.
 */
@Composable
private fun FormField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        content()
    }
}
