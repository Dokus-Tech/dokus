package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ChipShape = RoundedCornerShape(4.dp)

/**
 * Compact inline citation chips for AI chat responses.
 * Each chip shows: [N] DocumentName p.X — tappable to navigate to source.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatCitationChips(
    citations: List<ChatCitation>,
    onCitationClick: (ChatCitation) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (citations.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
    ) {
        citations.forEachIndexed { index, citation ->
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        ChipShape,
                    )
                    .border(Constraints.Stroke.thin, MaterialTheme.colorScheme.outlineVariant, ChipShape)
                    .clickable { onCitationClick(citation) }
                    .padding(horizontal = Constraints.Spacing.small, vertical = Constraints.Spacing.xxSmall),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = "[${index + 1}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
                )
                Text(
                    text = citation.documentName ?: "Document",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                citation.pageNumber?.let { page ->
                    Text(
                        text = "p.$page",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChatCitationChipsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatCitationChips(
            citations = listOf(
                ChatCitation(
                    chunkId = "1",
                    documentId = "doc-1",
                    documentName = "SRL Accounting & Tax",
                    pageNumber = 1,
                    excerpt = "Comptabilit\u00e9 & prestations",
                    relevanceScore = 0.95f,
                ),
                ChatCitation(
                    chunkId = "2",
                    documentId = "doc-2",
                    documentName = "Tesla Belgium BVBA",
                    pageNumber = 1,
                    excerpt = "Supercharging",
                    relevanceScore = 0.88f,
                ),
            ),
            onCitationClick = {},
        )
    }
}
