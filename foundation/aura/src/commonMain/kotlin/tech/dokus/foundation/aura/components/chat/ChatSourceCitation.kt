package tech.dokus.foundation.aura.components.chat

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_collapse_citation
import tech.dokus.aura.resources.chat_collapse_sources
import tech.dokus.aura.resources.chat_document_fallback
import tech.dokus.aura.resources.chat_expand_citation
import tech.dokus.aura.resources.chat_expand_sources
import tech.dokus.aura.resources.chat_page_number
import tech.dokus.aura.resources.chat_sources_count
import tech.dokus.aura.resources.chat_view_source_document
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Data class representing a source citation for display.
 * Maps to the ChatCitation domain model.
 */
data class CitationDisplayData(
    /** ID of the document chunk this citation references */
    val chunkId: String,
    /** ID of the source document */
    val documentId: String,
    /** Human-readable document name for display */
    val documentName: String? = null,
    /** Page number in the original document (if applicable) */
    val pageNumber: Int? = null,
    /** Excerpt from the chunk that was used */
    val excerpt: String,
    /** Confidence/relevance score for this citation (0.0 - 1.0) */
    val relevanceScore: Float? = null
)

/**
 * Default values for ChatSourceCitation components.
 */
object ChatSourceCitationDefaults {
    val maxExcerptLines = 4
    val iconSize = 18.dp
}

/**
 * An expandable source citation component that displays document references
 * in AI chat responses. Shows a collapsed header that can be expanded to
 * reveal the source excerpt.
 *
 * @param citation The citation data to display
 * @param modifier Optional modifier for the component
 * @param initiallyExpanded Whether the citation should start in expanded state
 * @param onDocumentClick Optional callback when the document reference is clicked
 */
@Composable
fun ChatSourceCitation(
    citation: CitationDisplayData,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    onDocumentClick: ((documentId: String) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        // Header row (always visible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(
                    horizontal = Constrains.Spacing.medium,
                    vertical = Constrains.Spacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(ChatSourceCitationDefaults.iconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Constrains.Spacing.small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = citation.documentName ?: stringResource(Res.string.chat_document_fallback),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (citation.pageNumber != null) {
                        Text(
                            text = stringResource(Res.string.chat_page_number, citation.pageNumber),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Relevance score badge (if available)
            if (citation.relevanceScore != null) {
                RelevanceScoreBadge(
                    score = citation.relevanceScore,
                    modifier = Modifier.padding(end = Constrains.Spacing.small)
                )
            }

            // Expand/collapse icon
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) Res.string.chat_collapse_citation
                    else Res.string.chat_expand_citation
                ),
                modifier = Modifier
                    .size(Constrains.IconSize.medium)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expandable excerpt section
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Constrains.Spacing.medium,
                        end = Constrains.Spacing.medium,
                        bottom = Constrains.Spacing.medium
                    )
            ) {
                // Divider
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(Constrains.Spacing.small))

                // Excerpt text
                Text(
                    text = citation.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = ChatSourceCitationDefaults.maxExcerptLines,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.shapes.extraSmall
                        )
                        .padding(Constrains.Spacing.small)
                )

                // View document link (if callback provided)
                if (onDocumentClick != null) {
                    Spacer(modifier = Modifier.height(Constrains.Spacing.small))
                    Text(
                        text = stringResource(Res.string.chat_view_source_document),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onDocumentClick(citation.documentId) }
                            .padding(vertical = Constrains.Spacing.xSmall)
                    )
                }
            }
        }
    }
}

/**
 * A small badge showing the relevance score of a citation.
 * Higher scores show in a more prominent color.
 *
 * @param score The relevance score (0.0 - 1.0)
 * @param modifier Optional modifier for the badge
 */
@Composable
private fun RelevanceScoreBadge(
    score: Float,
    modifier: Modifier = Modifier
) {
    val displayScore = (score * 100).toInt()
    val backgroundColor = when {
        score >= 0.8f -> MaterialTheme.colorScheme.primaryContainer
        score >= 0.6f -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        score >= 0.8f -> MaterialTheme.colorScheme.onPrimaryContainer
        score >= 0.6f -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = stringResource(Res.string.common_percent_value, displayScore),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .background(backgroundColor, MaterialTheme.shapes.extraSmall)
            .padding(
                horizontal = Constrains.Spacing.xSmall,
                vertical = Constrains.Spacing.xxSmall
            )
    )
}

/**
 * A list of expandable source citations with a header.
 * Shows a compact "Sources (N)" header that expands to show all citations.
 *
 * @param citations The list of citations to display
 * @param modifier Optional modifier for the component
 * @param onDocumentClick Optional callback when a document reference is clicked
 */
@Composable
fun ChatSourceCitationList(
    citations: List<CitationDisplayData>,
    modifier: Modifier = Modifier,
    onDocumentClick: ((documentId: String) -> Unit)? = null
) {
    if (citations.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "list_expand_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(Constrains.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.chat_sources_count, citations.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) Res.string.chat_collapse_sources
                    else Res.string.chat_expand_sources
                ),
                modifier = Modifier
                    .size(Constrains.IconSize.medium)
                    .rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Expandable citations list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Constrains.Spacing.medium,
                        end = Constrains.Spacing.medium,
                        bottom = Constrains.Spacing.medium
                    ),
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                citations.forEachIndexed { index, citation ->
                    ChatSourceCitation(
                        citation = citation,
                        onDocumentClick = onDocumentClick
                    )
                }
            }
        }
    }
}

/**
 * Convenience composable for a single source citation with minimal styling.
 * Use when showing inline citations directly after a message.
 *
 * @param documentName The name of the source document
 * @param pageNumber Optional page number reference
 * @param modifier Optional modifier for the component
 * @param onClick Optional callback when the citation is clicked
 */
@Composable
fun PInlineCitation(
    documentName: String,
    pageNumber: Int? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .padding(
                horizontal = Constrains.Spacing.small,
                vertical = Constrains.Spacing.xSmall
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(Constrains.IconSize.xSmall),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(Constrains.Spacing.xSmall))
        Text(
            text = if (pageNumber != null) "$documentName, p.$pageNumber" else documentName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
