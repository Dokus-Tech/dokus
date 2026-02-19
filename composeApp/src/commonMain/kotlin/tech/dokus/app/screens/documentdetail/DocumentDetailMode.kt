package tech.dokus.app.screens.documentdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent

private val QueueWindowWidth = 220.dp

/**
 * Document detail mode — full shell takeover replacing sidebar + content.
 * Shows a document queue (left) and the document review content (right).
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

    Box(
        Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()
        Row(Modifier.fillMaxSize().padding(Constrains.Shell.padding)) {
            // Queue window (glass panel, same styling as sidebar)
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

            // Content window (glass content panel)
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
                content()
            }
        }
    }
}
