package tech.dokus.app.screens.documentdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.textMuted

private val QueueWindowWidth = 220.dp

/**
 * Document detail mode — full shell takeover replacing sidebar + content.
 * Shows a document queue (left) and the document review content (right).
 *
 * JSX: DocumentDetailMode component — display flex, height 100%, gap 10, padding 10.
 * Queue = 220dp glass panel. Content = flex:1 glassContent panel.
 */
@Composable
internal fun DocumentDetailMode(
    documents: List<DocQueueItem>,
    selectedDocumentId: DocumentId,
    onSelectDocument: (DocumentId) -> Unit,
    onExit: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val selectedDoc = documents.firstOrNull { it.id == selectedDocumentId }

    // JSX: display flex, height 100%, background C.bg, position relative
    Box(
        Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()

        // JSX: display flex, flex 1, gap 10, padding 10, position relative, zIndex 1
        Row(
            Modifier
                .fillMaxSize()
                .padding(Constrains.Shell.padding)
        ) {
            // LEFT: Queue window — JSX: width 220, glass background, borderRadius 16
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(QueueWindowWidth),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.glass,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                DocumentQueueWindow(
                    documents = documents,
                    selectedDocumentId = selectedDocumentId,
                    onSelectDocument = onSelectDocument,
                    onExit = onExit,
                )
            }

            // RIGHT: Content window — JSX: flex 1, glassContent, borderRadius 16
            // overflow hidden, display flex, flexDirection column
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Constrains.Shell.gap),
                color = colorScheme.glassContent,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Title bar — JSX: padding "10px 18px", borderBottom, background glassHeader
                    DetailModeTitleBar(
                        vendorName = selectedDoc?.vendorName ?: "\u2014",
                        amount = selectedDoc?.amount ?: "",
                        isConfirmed = selectedDoc?.isConfirmed ?: false,
                    )

                    // Content (NavHost rendering DocumentReviewRoute)
                    CompositionLocalProvider(LocalIsInDocDetailMode provides true) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Title bar for the content window in document detail mode.
 *
 * JSX: padding "10px 18px", borderBottom "1px solid rgba(0,0,0,0.05)",
 * background C.glassHeader. Left: vendor(14sp,700) + amount(11sp,mono,textMuted).
 * Right: StatusDot(5dp) + status text(10sp,600).
 */
@Composable
private fun DetailModeTitleBar(
    vendorName: String,
    amount: String,
    isConfirmed: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val statusColor = if (isConfirmed) colorScheme.tertiary else colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.glassHeader)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: vendor name + amount
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = vendorName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                letterSpacing = (-0.02).sp,
            )
            Text(
                text = amount,
                fontSize = 11.sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                color = colorScheme.textMuted,
            )
        }

        // Right: status dot + status text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusDot(
                type = if (isConfirmed) StatusDotType.Confirmed else StatusDotType.Warning,
                size = 5.dp,
            )
            Text(
                text = if (isConfirmed) "Confirmed" else "Needs review",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
        }
    }

    // JSX: borderBottom "1px solid rgba(0,0,0,0.05)"
    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
}
