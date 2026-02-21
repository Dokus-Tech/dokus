package tech.dokus.app.screens.documentdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.a11y_back_to_all_documents
import tech.dokus.aura.resources.document_queue_all_docs
import tech.dokus.aura.resources.document_queue_navigate
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Document queue window — left 220dp glass panel in document detail mode.
 * Shows back button, position counter, and scrollable document list.
 *
 * Matches JSX: DocumentDetailMode queue section.
 */
@Composable
internal fun DocumentQueueWindow(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    onSelectDocument: (DocumentId) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = documents.indexOfFirst { it.id == selectedDocumentId }
    val positionText = if (selectedIndex >= 0) "${selectedIndex + 1}/${documents.size}" else ""

    Column(modifier = modifier.fillMaxHeight()) {
        // Header: back button + position counter
        // JSX: padding "12px 14px 10px", borderBottom "1px solid rgba(0,0,0,0.06)"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // JSX: chevron SVG + "All docs", 12sp 500 weight, amber color, gap 4
            val backDescription = stringResource(Res.string.a11y_back_to_all_documents)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .semantics { contentDescription = backDescription }
                    .clickable(onClick = onExit),
            ) {
                Text(
                    text = "\u2039",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.document_queue_all_docs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // JSX: mono, 10sp, textFaint, marginLeft auto
            Text(
                text = positionText,
                fontSize = 10.sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }

        // Header bottom border — JSX: 1px solid rgba(0,0,0,0.06)
        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))

        // Document list
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(
                items = documents,
                key = { _, item -> item.id.toString() }
            ) { _, item ->
                val isSelected = item.id == selectedDocumentId
                QueueDocumentItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onSelectDocument(item.id) },
                )
            }
        }

        // Footer border — JSX: 1px solid rgba(0,0,0,0.06), padding 7px 14px
        HorizontalDivider(color = Color.Black.copy(alpha = 0.06f))

        // Keyboard hint — JSX: key boxes with borders + "navigate" text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val faintColor = MaterialTheme.colorScheme.textFaint
            val monoFamily = MaterialTheme.typography.labelLarge.fontFamily
            // Key box for ↑
            Text(
                text = "\u2191",
                fontSize = 8.sp,
                fontFamily = monoFamily,
                color = faintColor,
                modifier = Modifier
                    .border(1.dp, faintColor, RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            Spacer(Modifier.width(3.dp))
            // Key box for ↓
            Text(
                text = "\u2193",
                fontSize = 8.sp,
                fontFamily = monoFamily,
                color = faintColor,
                modifier = Modifier
                    .border(1.dp, faintColor, RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.document_queue_navigate),
                fontSize = 9.sp,
                color = faintColor,
            )
        }
    }
}

@Composable
private fun QueueDocumentItem(
    item: DocQueueItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val warmBg = MaterialTheme.colorScheme.surfaceHover
    val amberColor = MaterialTheme.colorScheme.primary
    // JSX: borderBottom "1px solid rgba(0,0,0,0.03)"
    val itemBorderColor = Color.Black.copy(alpha = 0.03f)

    // JSX: padding "10px 14px", gap 8, flex row with dot, text column, amount
    // Selected: background warm, borderRight 2px solid amber
    // Non-selected: borderRight "2px solid transparent" (for consistent sizing)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .background(warmBg)
                        .drawWithContent {
                            drawContent()
                            // 2dp amber right border
                            drawRect(
                                color = amberColor,
                                topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                                size = Size(2.dp.toPx(), size.height),
                            )
                        }
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Status dot — JSX: 5dp
        StatusDot(
            type = if (item.isConfirmed) StatusDotType.Confirmed else StatusDotType.Warning,
            size = 5.dp,
        )

        // Text column — JSX: flex 1, minWidth 0
        Column(modifier = Modifier.weight(1f)) {
            // Vendor name — JSX: 11sp, fontWeight 600 when selected else 400
            Text(
                text = item.vendorName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Date — JSX: 9sp mono textMuted, marginTop 1
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.date,
                fontSize = 9.sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        // Amount — JSX: 11sp mono 500 weight, text color, flexShrink 0
        Text(
            text = item.amount,
            fontSize = 11.sp,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    // Item bottom border — JSX: 1px solid rgba(0,0,0,0.03)
    HorizontalDivider(color = itemBorderColor)
}

// =============================================================================
// Previews
// =============================================================================

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Preview
@Composable
private fun DocumentQueueWindowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val docId1 = DocumentId(kotlin.uuid.Uuid.random())
    val docId2 = DocumentId(kotlin.uuid.Uuid.random())
    val docId3 = DocumentId(kotlin.uuid.Uuid.random())
    val sampleDocuments = listOf(
        DocQueueItem(
            id = docId1,
            vendorName = "Acme Corp",
            amount = "1,250.00",
            date = "Feb 15",
            isConfirmed = false,
        ),
        DocQueueItem(
            id = docId2,
            vendorName = "Tech Solutions",
            amount = "890.50",
            date = "Feb 14",
            isConfirmed = true,
        ),
        DocQueueItem(
            id = docId3,
            vendorName = "Cloud Services Ltd",
            amount = "3,200.00",
            date = "Feb 13",
            isConfirmed = false,
        ),
    )
    TestWrapper(parameters) {
        DocumentQueueWindow(
            documents = sampleDocuments,
            selectedDocumentId = docId1,
            onSelectDocument = {},
            onExit = {},
        )
    }
}
