package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_download_all_zip
import tech.dokus.aura.resources.chat_n_documents
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.DocumentReferenceDto
import tech.dokus.domain.model.ai.DocumentReferenceType
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * List of document cards with optional "Download all as ZIP" header.
 */
@Composable
fun ChatDocumentCardList(
    block: ChatContentBlock.Documents,
    onDownloadSingle: (DocumentReferenceDto) -> Unit,
    onDownloadAll: () -> Unit,
    onDocumentClick: (DocumentReferenceDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        // Header with count + ZIP download
        if (block.showDownloadAll && block.items.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constraints.Spacing.xSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.chat_n_documents, block.items.size),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                PPrimaryButton(
                    text = stringResource(Res.string.chat_download_all_zip),
                    onClick = onDownloadAll,
                )
            }
        }

        // Document cards
        val compact = block.items.size > 3
        block.items.forEach { doc ->
            ChatDocumentCard(
                doc = doc,
                onDownload = { onDownloadSingle(doc) },
                onClick = { onDocumentClick(doc) },
                compact = compact,
            )
        }
    }
}

@Preview
@Composable
private fun ChatDocumentCardListPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatDocumentCardList(
            block = ChatContentBlock.Documents(
                items = listOf(
                    DocumentReferenceDto(name = "SRL Accounting & Tax", ref = "20260050", type = DocumentReferenceType.Invoice, amount = "\u20ac798.60"),
                    DocumentReferenceDto(name = "Tesla Belgium BVBA", ref = "peppol-71b40a13", type = DocumentReferenceType.Invoice, amount = "\u20ac346.97"),
                ),
                showDownloadAll = true,
            ),
            onDownloadSingle = {},
            onDownloadAll = {},
            onDocumentClick = {},
        )
    }
}
