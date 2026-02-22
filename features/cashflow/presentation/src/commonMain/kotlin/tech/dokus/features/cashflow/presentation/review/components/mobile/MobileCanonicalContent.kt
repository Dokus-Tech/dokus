package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun MobileCanonicalContent(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accordion = rememberSaveable { mutableStateMapOf("items" to true, "sources" to false, "bank" to false, "notes" to false) }
    val counterparty = counterpartyInfo(state)
    val currencySign = currencySign(state)
    val lineItems = lineItems(state)
    val amount = state.totalAmount?.toDisplayString() ?: "\u2014"
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(Constraints.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        MobileTopHeader(
            vendor = counterparty.name ?: "Unknown vendor",
            status = state.financialStatus,
            onBackClick = onBackClick,
        )

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = "$currencySign$amount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = state.primaryDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    MetaCell("Issued", state.issueDate() ?: "\u2014")
                    if (state.draftData is InvoiceDraftData) {
                        MetaCell("Due", state.dueDate() ?: "\u2014")
                    }
                    MetaCell("Invoice", state.referenceNumber() ?: "\u2014")
                }
            }
        }

        MobileStateCard(state = state, onIntent = onIntent)

        MobileAccordion(
            title = "Items",
            count = lineItems.size,
            expanded = accordion["items"] == true,
            onToggle = { accordion["items"] = accordion["items"] != true },
        ) {
            if (lineItems.isEmpty()) {
                ValueText("No line items")
            } else {
                lineItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = item.description.ifBlank { "\u2014" },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = lineAmount(item, currencySign),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Constraints.Spacing.small),
                        )
                    }
                }
                TotalsBlock(state = state, currencySign = currencySign)
            }
        }

        MobileAccordion(
            title = "Sources",
            count = state.document.sources.size,
            expanded = accordion["sources"] == true,
            onToggle = { accordion["sources"] = accordion["sources"] != true },
        ) {
            state.document.sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(DocumentReviewIntent.OpenSourceModal(source.id)) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = source.sourceChannel.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (source.sourceChannel == tech.dokus.domain.enums.DocumentSource.Peppol) {
                            MaterialTheme.colorScheme.statusWarning
                        } else {
                            MaterialTheme.colorScheme.textMuted
                        },
                    )
                    Text(
                        text = source.filename ?: "Original document",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Constraints.Spacing.small),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("\u203A", color = MaterialTheme.colorScheme.textMuted)
                }
            }
        }

        val bank = state.bankDetails()
        if (!bank.isNullOrBlank()) {
            MobileAccordion(
                title = "Bank details",
                expanded = accordion["bank"] == true,
                onToggle = { accordion["bank"] = accordion["bank"] != true },
            ) {
                ValueText(bank)
            }
        }

        val notes = state.notes()
        if (!notes.isNullOrBlank()) {
            MobileAccordion(
                title = "Notes",
                expanded = accordion["notes"] == true,
                onToggle = { accordion["notes"] = accordion["notes"] != true },
            ) {
                ValueText(notes)
            }
        }
    }
}

@Composable
private fun MobileTopHeader(
    vendor: String,
    status: ReviewFinancialStatus,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onBackClick)
            ) {
                PIcon(icon = FeatherIcons.ArrowLeft, description = "Back")
                Text(
                    text = "Documents",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.statusWarning,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                text = vendor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = status.color().copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = Constraints.Spacing.small, vertical = 6.dp)
        ) {
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelMedium,
                color = status.color(),
            )
        }
    }
}

@Composable
private fun MobileStateCard(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            val statusTitle = when (state.financialStatus) {
                ReviewFinancialStatus.Paid -> "Payment received"
                ReviewFinancialStatus.Unpaid -> "Awaiting payment"
                ReviewFinancialStatus.Overdue -> "Overdue"
                ReviewFinancialStatus.Review -> "Review required"
            }
            Text(text = statusTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            val subtitle = when (state.financialStatus) {
                ReviewFinancialStatus.Paid -> {
                    val entry = (state.cashflowEntryState as? DokusState.Success<*>)?.data as? tech.dokus.domain.model.CashflowEntry
                    val paidAt = entry?.paidAt
                    if (paidAt != null) "${paidAt.date} \u00B7 Bank transfer" else "Payment recorded"
                }
                ReviewFinancialStatus.Unpaid -> "Record payment when money is received."
                ReviewFinancialStatus.Overdue -> "This invoice is overdue."
                ReviewFinancialStatus.Review -> "Verify extracted details and confirm."
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )

            when (state.financialStatus) {
                ReviewFinancialStatus.Review -> {
                    Button(
                        onClick = { onIntent(DocumentReviewIntent.Confirm) },
                        enabled = state.canConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Confirm document")
                    }
                }
                ReviewFinancialStatus.Unpaid,
                ReviewFinancialStatus.Overdue -> {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        enabled = state.canRecordPayment,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Record payment")
                    }
                }
                ReviewFinancialStatus.Paid -> Unit
            }
        }
    }
}

@Composable
private fun MobileAccordion(
    title: String,
    count: Int? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (count != null) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
                Text(
                    text = if (expanded) "\u2303" else "\u2304",
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun MetaCell(
    label: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(90.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TotalsBlock(state: DocumentReviewState.Content, currencySign: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ValueText("Subtotal ${state.subtotalAmount()?.let { "$currencySign${it.toDisplayString()}" } ?: "\u2014"}")
        ValueText("VAT ${state.vatAmount()?.let { "$currencySign${it.toDisplayString()}" } ?: "\u2014"}")
        Text(
            text = "Total $currencySign${state.totalAmount?.toDisplayString() ?: "\u2014"}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ValueText(value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
