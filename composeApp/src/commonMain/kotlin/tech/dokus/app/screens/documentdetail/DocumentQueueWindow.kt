package tech.dokus.app.screens.documentdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

/**
 * Document queue window — left 220dp glass panel in document detail mode.
 * Shows back button, position counter, and scrollable document list.
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\u2039 All docs",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onExit),
            )
            Text(
                text = positionText,
                fontSize = 10.sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }

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

        // Keyboard hint footer
        Text(
            text = "\u2191\u2193 navigate",
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            textAlign = TextAlign.Center,
            fontSize = 9.sp,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = MaterialTheme.colorScheme.textFaint,
        )
    }
}

@Composable
private fun QueueDocumentItem(
    item: DocQueueItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val warmBg = MaterialTheme.colorScheme.surfaceHover
    val borderAmberColor = MaterialTheme.colorScheme.borderAmber

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isSelected) {
                        Modifier.drawWithContent {
                            drawContent()
                            drawRect(
                                color = borderAmberColor,
                                topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                                size = Size(2.dp.toPx(), size.height)
                            )
                        }
                    } else Modifier
                )
                .clickable(onClick = onClick)
                .then(
                    if (isSelected) {
                        Modifier.drawWithContent {
                            drawRect(color = warmBg)
                            drawContent()
                        }
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            StatusDot(
                type = if (item.isConfirmed) StatusDotType.Confirmed else StatusDotType.Warning,
                size = 5.dp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.vendorName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.date,
                        fontSize = 9.sp,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        color = MaterialTheme.colorScheme.textFaint,
                    )
                    Text(
                        text = item.amount,
                        fontSize = 10.sp,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.textMuted
                        },
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 14.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
