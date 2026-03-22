package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_needs_review
import tech.dokus.aura.resources.review_surface_view_full_detail
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.sortDate
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewIntent
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailQueueState
import tech.dokus.foundation.aura.components.badges.SourceBadge
import tech.dokus.foundation.aura.components.badges.toUiSource
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.style.textMuted

/**
 * Top-level review surface composable for desktop.
 *
 * Replaces both the center PDF pane and the inspector pane when the
 * document is in review mode (NeedsReview status, non-bank-statement).
 *
 * Contains:
 * - Header with status dot + "View full detail" link
 * - Document thumbnail card (left) + decision stream (right)
 * - Keyboard shortcut hints at the bottom
 * - PDF zoom overlay (toggled by Z key or clicking thumbnail)
 */
@Composable
internal fun DesktopReviewSurface(
    state: DocumentDetailState,
    isAccountantReadOnly: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentDetailIntent) -> Unit,
    onCorrectContact: (ResolvedContact) -> Unit,
    onCreateContact: (ResolvedContact) -> Unit,
    onSwitchToDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val issues = remember(state.draftData, state.effectiveContact, state.contactMatchStatus) {
        state.deriveReviewIssues()
    }
    var activeIssueIndex by remember(issues) { mutableIntStateOf(0) }
    var showZoom by remember { mutableStateOf(false) }

    val actionType = resolveActionType(issues, activeIssueIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                handleKeyEvent(
                    key = event.key,
                    state = state,
                    issues = issues,
                    showZoom = showZoom,
                    onToggleZoom = { showZoom = !showZoom },
                    onSwitchToDetail = onSwitchToDetail,
                    onIntent = onIntent,
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            // Header: status dot + "View full detail →"
            ReviewHeader(
                onViewDetail = onSwitchToDetail,
            )

            // Main content: thumbnail (left) + decision stream (right)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 780.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xLarge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Document thumbnail — PDF only, no text
                ReviewDocumentCard(
                    previewState = state.previewState,
                    onZoomClick = { showZoom = true },
                )

                // Right: Identity + decision stream (scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                ) {
                    // Document identity
                    ReviewIdentityBlock(
                        vendorName = resolveVendorName(state),
                        totalAmount = resolveDisplayAmount(state),
                        dateDisplay = state.draftData?.sortDate?.toString() ?: "",
                    )

                    // Source badges
                    ReviewSourceBadges(state = state)

                    // Decision stream
                    ReviewDecisionStream(
                        state = state,
                        issues = issues,
                        activeIssueIndex = activeIssueIndex,
                        onIntent = onIntent,
                        onChooseDifferent = {
                            onIntent(DocumentDetailIntent.OpenContactSheet)
                        },
                    )

                    Spacer(modifier = Modifier.height(Constraints.Spacing.small))

                    // Action footer
                    val hasContactIssue = issues.getOrNull(activeIssueIndex) is ReviewIssue.ContactIssue
                    if (!isAccountantReadOnly) {
                        ReviewActionFooter(
                            actionType = actionType,
                            isEnabled = if (issues.isEmpty()) state.canConfirm else true,
                            isLoading = state.isConfirming,
                            showChooseDifferent = hasContactIssue,
                            onPrimaryAction = {
                                handlePrimaryAction(
                                    issues = issues,
                                    activeIssueIndex = activeIssueIndex,
                                    onIntent = onIntent,
                                )
                            },
                            onChooseDifferent = {
                                onIntent(DocumentDetailIntent.OpenContactSheet)
                            },
                            onReviewLater = {
                                val nextId = state.queueState?.nextDocumentId(state.selectedQueueDocumentId ?: state.documentId)
                                if (nextId != null) {
                                    onIntent(DocumentDetailIntent.SelectQueueDocument(nextId))
                                }
                            },
                        )
                    }
                }
            }

            // Bottom: keyboard hints
            ReviewKeyboardHints(
                canConfirm = state.canConfirm || issues.isEmpty(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            // Bottom: "View full detail →"
            Text(
                text = stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onSwitchToDetail),
            )
        }

        // PDF zoom overlay
        PdfZoomOverlay(
            visible = showZoom,
            previewState = state.previewState,
            onDismiss = { showZoom = false },
            onLoadMore = { maxPages ->
                onIntent(DocumentDetailIntent.Preview(DocumentPreviewIntent.LoadMorePages(maxPages)))
            },
        )
    }
}

@Composable
private fun ReviewHeader(
    onViewDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            StatusDot(type = StatusDotType.Warning, size = 8.dp)
            Text(
                text = stringResource(Res.string.review_surface_needs_review),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.clickable(onClick = onViewDetail),
        )
    }
}

@Composable
private fun ReviewSourceBadges(
    state: DocumentDetailState,
    modifier: Modifier = Modifier,
) {
    val sources = state.documentRecord?.sources.orEmpty()
    if (sources.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sources.forEach { source ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SourceBadge(source = source.sourceChannel.toUiSource())
                Text(
                    text = "\u00B7 ${source.arrivalAt.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

/**
 * Resolve a short source label for the document card (e.g., "PDF" or "PEPPOL").
 */
@Composable
private fun resolveSourceLabel(state: DocumentDetailState): String {
    val source = state.documentRecord?.sources?.firstOrNull()?.sourceChannel ?: return "PDF"
    return source.localizedUppercase
}

/**
 * Handle primary action button click.
 */
private fun handlePrimaryAction(
    issues: List<ReviewIssue>,
    activeIssueIndex: Int,
    onIntent: (DocumentDetailIntent) -> Unit,
) {
    if (issues.isEmpty()) {
        onIntent(DocumentDetailIntent.Confirm)
        return
    }

    val activeIssue = issues.getOrNull(activeIssueIndex) ?: return

    when (activeIssue) {
        is ReviewIssue.ContactIssue -> {
            // Accept suggested contact, then confirm will follow from state recomposition
            onIntent(DocumentDetailIntent.AcceptSuggestedContact)
        }
        is ReviewIssue.DirectionIssue,
        is ReviewIssue.AmountIssue,
        is ReviewIssue.DateIssue -> {
            // These issues resolve via field edits — confirm when all resolved
            if (activeIssueIndex >= issues.lastIndex) {
                onIntent(DocumentDetailIntent.Confirm)
            }
        }
    }
}

/**
 * Handle keyboard events for the review surface.
 *
 * @return true if the event was consumed
 */
private fun handleKeyEvent(
    key: Key,
    state: DocumentDetailState,
    issues: List<ReviewIssue>,
    showZoom: Boolean,
    onToggleZoom: () -> Unit,
    onSwitchToDetail: () -> Unit,
    onIntent: (DocumentDetailIntent) -> Unit,
): Boolean {
    when (key) {
        Key.Escape -> {
            if (showZoom) {
                onToggleZoom()
                return true
            }
            return false
        }
        Key.Z -> {
            onToggleZoom()
            return true
        }
        Key.D -> {
            onSwitchToDetail()
            return true
        }
        Key.Enter -> {
            if (state.canConfirm || issues.isEmpty()) {
                handlePrimaryAction(issues, 0, onIntent)
                return true
            }
            return false
        }
        Key.DirectionUp, Key.K -> {
            val prevId = state.queueState?.previousDocumentId(state.selectedQueueDocumentId ?: state.documentId)
            if (prevId != null) {
                onIntent(DocumentDetailIntent.SelectQueueDocument(prevId))
                return true
            }
            return false
        }
        Key.DirectionDown, Key.J -> {
            val nextId = state.queueState?.nextDocumentId(state.selectedQueueDocumentId ?: state.documentId)
            if (nextId != null) {
                onIntent(DocumentDetailIntent.SelectQueueDocument(nextId))
                return true
            }
            return false
        }
        else -> return false
    }
}

// =============================
// Queue Navigation Helpers
// =============================

private fun DocumentDetailQueueState.nextDocumentId(currentId: DocumentId?): DocumentId? {
    if (currentId == null) return items.firstOrNull()?.id
    val currentIndex = items.indexOfFirst { it.id == currentId }
    if (currentIndex < 0) return items.firstOrNull()?.id
    return items.getOrNull(currentIndex + 1)?.id
}

private fun DocumentDetailQueueState.previousDocumentId(currentId: DocumentId?): DocumentId? {
    if (currentId == null) return null
    val currentIndex = items.indexOfFirst { it.id == currentId }
    if (currentIndex <= 0) return null
    return items.getOrNull(currentIndex - 1)?.id
}
