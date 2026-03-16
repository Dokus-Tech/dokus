package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import tech.dokus.domain.model.ai.ChatAttachedFile
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ChipShape = RoundedCornerShape(6.dp)

/**
 * Horizontal chips showing files attached to the chat conversation.
 * Each chip shows filename + X button to remove.
 * Shown above the input bar when files are attached.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatAttachedFileChips(
    files: List<ChatAttachedFile>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (files.isEmpty()) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Constraints.Spacing.xxLarge, vertical = Constraints.Spacing.xSmall),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        files.forEach { file ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.amberSoft, ChipShape)
                    .border(Constraints.Stroke.thin, MaterialTheme.colorScheme.borderAmber, ChipShape)
                    .padding(start = Constraints.Spacing.small, end = Constraints.Spacing.xxSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = file.filename,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (file.isUploading) {
                    Text(
                        text = "...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                } else {
                    IconButton(
                        onClick = { onRemove(file.refId) },
                        modifier = Modifier.size(18.dp),
                    ) {
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = "Remove",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChatAttachedFileChipsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatAttachedFileChips(
            files = listOf(
                ChatAttachedFile(refId = "1", filename = "invoice-q4-2025.pdf"),
                ChatAttachedFile(refId = "2", filename = "receipt-amazon.jpg", isUploading = true),
            ),
            onRemove = {},
        )
    }
}
