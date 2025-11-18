package ai.dokus.app.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.dokus.foundation.domain.model.FinancialDocument
import ai.dokus.foundation.domain.model.FinancialDocumentStatus

/**
 * A card component displaying a cash flow list with financial document items and navigation controls.
 *
 * @param documents List of financial documents to display
 * @param onPreviousClick Callback when the previous arrow button is clicked
 * @param onNextClick Callback when the next arrow button is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun CashflowCard(
    documents: List<FinancialDocument>,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = "Cash flow",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Document items list
            documents.forEachIndexed { index, document ->
                CashflowDocumentItem(document = document)

                // Add divider between items (not after the last item)
                if (index < documents.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous button
                FilledIconButton(
                    onClick = onPreviousClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous"
                    )
                }

                // Next button
                FilledIconButton(
                    onClick = onNextClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next"
                    )
                }
            }
        }
    }
}

/**
 * A single cash flow document item row displaying document number and status badge.
 *
 * @param document The financial document to display
 * @param modifier Optional modifier for the row
 */
@Composable
private fun CashflowDocumentItem(
    document: FinancialDocument,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document number with optional icon
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document type icon (optional)
            Text(
                text = document.typeIcon(),
                style = MaterialTheme.typography.bodyMedium
            )

            // Document number
            Text(
                text = document.documentNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Status badge
        StatusBadge(status = document.status)
    }
}

/**
 * A badge component for displaying document status with colored background.
 *
 * @param status The document status to display
 * @param modifier Optional modifier for the badge
 */
@Composable
private fun StatusBadge(
    status: FinancialDocumentStatus,
    modifier: Modifier = Modifier
) {
    // Determine colors and text based on status using Material Theme
    val (backgroundColor, textColor, statusText) = when (status) {
        FinancialDocumentStatus.PendingApproval ->
            Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                "Need confirmation"
            )

        FinancialDocumentStatus.Approved ->
            Triple(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                "Approved"
            )

        FinancialDocumentStatus.Rejected ->
            Triple(
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                "Rejected"
            )

        FinancialDocumentStatus.Draft ->
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Draft"
            )

        FinancialDocumentStatus.Completed ->
            Triple(
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                "Completed"
            )

        FinancialDocumentStatus.Cancelled ->
            Triple(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
                "Cancelled"
            )
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

/**
 * Extension function to get the document icon/emoji representation.
 */
private fun FinancialDocument.typeIcon(): String = when (this) {
    is FinancialDocument.InvoiceDocument -> "📄"
    is FinancialDocument.ExpenseDocument -> "🧾"
}
