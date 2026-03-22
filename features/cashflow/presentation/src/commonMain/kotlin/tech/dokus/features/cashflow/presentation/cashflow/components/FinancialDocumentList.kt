package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_table_view_details
import tech.dokus.domain.model.DocDto
import tech.dokus.foundation.aura.components.CashflowTypeBadge
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing

/**
 * Mobile-friendly list component for displaying financial documents.
 * Shows simplified rows with Invoice #, Amount, and Type.
 *
 * @param documents List of financial documents to display
 * @param onDocumentClick Callback when a document row is clicked
 * @param modifier Optional modifier for the list
 */
@Composable
fun FinancialDocumentList(
    documents: List<DocDto>,
    onDocumentClick: (DocDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizing = MaterialTheme.dokusSizing
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            documents.forEachIndexed { index, document ->
                key(document.stableKey()) {
                    FinancialDocumentListItem(
                        row = document.toTableRow(),
                        onClick = { onDocumentClick(document) }
                    )

                    if (index < documents.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = sizing.strokeThin
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mobile-friendly list item showing simplified document info.
 * Displays: Invoice # | Amount | Type badge
 */
@Composable
private fun FinancialDocumentListItem(
    row: FinancialDocumentRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Invoice number with optional alert
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (row.hasAlert) {
                Box(
                    modifier = Modifier
                        .size(sizing.strokeCropGuide * 2f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }

            Text(
                text = row.invoiceNumber,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(spacing.medium))

        // Amount
        Text(
            text = row.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(spacing.medium))

        // Type badge
        CashflowTypeBadge(type = row.cashflowType)

        Spacer(modifier = Modifier.width(spacing.small))

        // Chevron
        Icon(
            imageVector = Lucide.ChevronRight,
            contentDescription = stringResource(Res.string.document_table_view_details),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(sizing.buttonLoadingIcon)
        )
    }
}

// Preview skipped: Flaky IllegalStateException in parallel Roborazzi runs
// FinancialDocumentTable already has a preview in FinancialDocumentTablePreview.kt
