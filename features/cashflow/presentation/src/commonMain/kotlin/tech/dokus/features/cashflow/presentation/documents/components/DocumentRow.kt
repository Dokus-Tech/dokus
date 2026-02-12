package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.document_table_amount
import tech.dokus.aura.resources.document_table_date
import tech.dokus.aura.resources.documents_table_counterparty
import tech.dokus.aura.resources.documents_table_description
import tech.dokus.aura.resources.documents_view_details
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constrains

private val TableRowHeight = 56.dp
private val StatusDotSize = 8.dp

/**
 * Column specifications for the documents table.
 * New structure: Dot | Description | Counterparty | Amount | Date | Chevron
 */
private object DocumentTableColumns {
    val Dot = DokusTableColumnSpec(width = 32.dp, horizontalAlignment = Alignment.CenterHorizontally)
    val Description = DokusTableColumnSpec(weight = 1f)
    val Counterparty = DokusTableColumnSpec(width = 180.dp)
    val Amount = DokusTableColumnSpec(width = 100.dp, horizontalAlignment = Alignment.End)
    val Date = DokusTableColumnSpec(width = 100.dp)
    val Action = DokusTableColumnSpec(width = 40.dp, horizontalAlignment = Alignment.CenterHorizontally)
}

@Composable
internal fun DocumentTableHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = 40.dp,
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        // Empty cell for dot column
        DokusTableCell(DocumentTableColumns.Dot) {
            Spacer(modifier = Modifier.width(1.dp))
        }
        DokusTableCell(DocumentTableColumns.Description) {
            SubtleHeaderLabel(text = stringResource(Res.string.documents_table_description))
        }
        DokusTableCell(DocumentTableColumns.Counterparty) {
            SubtleHeaderLabel(text = stringResource(Res.string.documents_table_counterparty))
        }
        DokusTableCell(DocumentTableColumns.Amount) {
            SubtleHeaderLabel(
                text = stringResource(Res.string.document_table_amount),
                textAlign = TextAlign.End
            )
        }
        DokusTableCell(DocumentTableColumns.Date) {
            SubtleHeaderLabel(text = stringResource(Res.string.document_table_date))
        }
        DokusTableCell(DocumentTableColumns.Action) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun SubtleHeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun DocumentTableRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = resolveDescription(document)
    val counterparty = resolveCounterparty(document)
    val amount = extractAmount(document)
    val dateLabel = formatShortDate(extractDocumentDate(document))
    val needsAttention = computeNeedsAttention(document)
    val isProcessing = computeIsProcessing(document)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource)
            .alpha(if (isProcessing) 0.5f else 1f),
        minHeight = TableRowHeight,
        onClick = onClick,
        backgroundColor = if (isHovered) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        // Status dot
        DokusTableCell(DocumentTableColumns.Dot) {
            StatusDot(needsAttention = needsAttention)
        }

        // Description (primary text)
        DokusTableCell(DocumentTableColumns.Description) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontStyle = if (isProcessing) FontStyle.Italic else FontStyle.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Counterparty (secondary text)
        DokusTableCell(DocumentTableColumns.Counterparty) {
            Text(
                text = counterparty,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Amount (tabular numbers)
        DokusTableCell(DocumentTableColumns.Amount) {
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Date
        DokusTableCell(DocumentTableColumns.Date) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Chevron (only visible on hover)
        DokusTableCell(DocumentTableColumns.Action) {
            if (isHovered) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.documents_view_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun DocumentMobileRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = resolveDescription(document)
    val counterparty = resolveCounterparty(document)
    val amount = extractAmount(document)
    val dateLabel = formatShortDate(extractDocumentDate(document))
    val needsAttention = computeNeedsAttention(document)
    val isProcessing = computeIsProcessing(document)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .alpha(if (isProcessing) 0.5f else 1f)
            .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        // Status dot
        StatusDot(needsAttention = needsAttention)

        // Content column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
        ) {
            // Top row: Description + Amount
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontStyle = if (isProcessing) FontStyle.Italic else FontStyle.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Constrains.Spacing.small))
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom row: Counterparty + Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                Text(
                    text = counterparty,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Status indicator dot.
 * - Amber for documents needing attention
 * - Neutral (zinc) for all others
 */
@Composable
private fun StatusDot(
    needsAttention: Boolean,
    modifier: Modifier = Modifier
) {
    val dotColor = if (needsAttention) {
        // Amber for attention - using statusWarning color
        Color(0xFFD97706)
    } else {
        // Neutral zinc dot
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier
            .size(StatusDotSize)
            .background(color = dotColor, shape = CircleShape)
    )
}

// =============================================================================
// Document State Helpers
// =============================================================================

/**
 * Determines if a document needs user attention.
 * Documents need attention if they are:
 * - Processing or queued (temporary state)
 * - Failed ingestion
 * - Needs review
 */
internal fun computeNeedsAttention(document: DocumentRecordDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    val documentStatus = document.draft?.documentStatus

    return when {
        ingestionStatus == IngestionStatus.Failed -> true
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> true
        documentStatus == DocumentStatus.NeedsReview -> true
        else -> false
    }
}

/**
 * Determines if a document is currently processing.
 * Used for visual treatment (50% opacity, italic text).
 */
internal fun computeIsProcessing(document: DocumentRecordDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    return ingestionStatus == IngestionStatus.Processing ||
        ingestionStatus == IngestionStatus.Queued
}

// =============================================================================
// Data Extraction Helpers
// =============================================================================

/**
 * Resolves the primary description for a document.
 * Priority: AI-generated description > counterparty > filename
 */
@Composable
private fun resolveDescription(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    val ingestionStatus = document.latestIngestion?.status

    val aiDescription = document.draft?.aiDescription.nonBlank()
    if (aiDescription != null) return aiDescription

    // Get description from extracted data (invoices use notes field)
    val context = when (extractedData) {
        is InvoiceDraftData -> extractedData.notes.nonBlank()
            ?: extractedData.invoiceNumber.nonBlank()
        is ReceiptDraftData -> extractedData.notes.nonBlank()
            ?: extractedData.receiptNumber.nonBlank()
        is CreditNoteDraftData -> extractedData.notes.nonBlank()
            ?: extractedData.reason.nonBlank()
            ?: extractedData.creditNoteNumber.nonBlank()
        else -> null
    }

    // Get counterparty name
    val counterparty = when (extractedData) {
        is InvoiceDraftData -> when (extractedData.direction) {
            DocumentDirection.Inbound -> (extractedData.seller.name ?: extractedData.customerName).nonBlank()
            DocumentDirection.Outbound -> (extractedData.buyer.name ?: extractedData.customerName).nonBlank()
            DocumentDirection.Unknown ->
                (extractedData.customerName ?: extractedData.buyer.name ?: extractedData.seller.name).nonBlank()
        }
        is ReceiptDraftData -> extractedData.merchantName.nonBlank()
        is CreditNoteDraftData -> extractedData.counterpartyName.nonBlank()
        else -> null
    }

    return when {
        // If we have both counterparty and context, combine them
        counterparty != null && context != null -> "$counterparty — $context"
        // If we have context but no counterparty
        context != null -> context
        // If we have counterparty but no context
        counterparty != null -> counterparty
        // Processing state placeholder
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> "Processing document…"
        // Fallback to filename
        else -> document.document.filename?.takeIf { it.isNotBlank() }
            ?: stringResource(Res.string.common_unknown)
    }
}

/**
 * Resolves the counterparty name for display in secondary column.
 */
@Composable
private fun resolveCounterparty(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    return when (extractedData) {
        is InvoiceDraftData -> when (extractedData.direction) {
            DocumentDirection.Inbound -> (extractedData.seller.name ?: extractedData.customerName).nonBlank()
            DocumentDirection.Outbound -> (extractedData.buyer.name ?: extractedData.customerName).nonBlank()
            DocumentDirection.Unknown ->
                (extractedData.customerName ?: extractedData.buyer.name ?: extractedData.seller.name).nonBlank()
        }
        is ReceiptDraftData -> extractedData.merchantName.nonBlank()
        is CreditNoteDraftData -> extractedData.counterpartyName.nonBlank()
        else -> null
    }
        ?: "—"
}

@Composable
private fun extractAmount(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    val amount = when (extractedData) {
        is InvoiceDraftData -> extractedData.totalAmount
        is ReceiptDraftData -> extractedData.totalAmount
        is CreditNoteDraftData -> extractedData.totalAmount
        else -> null
    }
    val currency = when (extractedData) {
        is InvoiceDraftData -> extractedData.currency
        is ReceiptDraftData -> extractedData.currency
        is CreditNoteDraftData -> extractedData.currency
        else -> null
    }

    return if (amount != null) {
        "${currency?.displaySign ?: "\u20AC"}${amount.toDisplayStringSafe()}"
    } else {
        "—"
    }
}

private fun extractDocumentDate(document: DocumentRecordDto): LocalDate {
    val extractedData = document.draft?.extractedData
    return when (extractedData) {
        is InvoiceDraftData -> extractedData.issueDate
        is ReceiptDraftData -> extractedData.date
        is CreditNoteDraftData -> extractedData.issueDate
        else -> null
    }
        ?: document.document.uploadedAt.date
}

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

private fun Money.toDisplayStringSafe(): String {
    return runCatching { toDisplayString() }.getOrElse { "0.00" }
}
