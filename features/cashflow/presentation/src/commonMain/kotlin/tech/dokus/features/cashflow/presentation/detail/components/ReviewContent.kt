package tech.dokus.features.cashflow.presentation.detail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.RoundedCornerShape
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_awaiting_extraction
import tech.dokus.aura.resources.cashflow_loading_document
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentSourceDto
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.mobile.MobileCanonicalContent
import tech.dokus.features.cashflow.presentation.detail.components.mobile.MobileCanonicalHeader
import tech.dokus.features.cashflow.presentation.detail.components.mobile.MobileDocumentDetailTopBar
import tech.dokus.features.cashflow.presentation.detail.components.mobile.PreviewTabContent
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted

private const val SummaryTabId = "summary"
private val SourceTabsPanelShape = RoundedCornerShape(14.dp)
private val SourceTabShape = RoundedCornerShape(10.dp)
private val DesktopInspectorWidth = Constraints.DocumentDetail.inspectorWidth + 32.dp

@Composable
internal fun ReviewContent(
    state: DocumentDetailState,
    isLargeScreen: Boolean,
    isAccountantReadOnly: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentDetailIntent) -> Unit,
    onCorrectContact: (ResolvedContact) -> Unit,
    onCreateContact: (ResolvedContact) -> Unit,
    onBackClick: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
) {
    when {
        state.document.isError() -> ErrorContent(state, contentPadding)
        state.document.isLoading() || !state.hasContent -> LoadingContent(contentPadding)
        state.isAwaitingExtraction -> AwaitingExtractionContent(
            state = state,
            contentPadding = contentPadding,
            isLargeScreen = isLargeScreen
        )
        state.hasContent -> {
            if (isLargeScreen) {
                DesktopReviewContent(
                    state = state,
                    isAccountantReadOnly = isAccountantReadOnly,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onCorrectContact = onCorrectContact,
                    onCreateContact = onCreateContact,
                )
            } else {
                MobileReviewContent(
                    state = state,
                    isAccountantReadOnly = isAccountantReadOnly,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onBackClick = onBackClick,
                    onOpenSource = onOpenSource,
                )
            }
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            DokusLoader()
            Text(
                text = stringResource(Res.string.cashflow_loading_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AwaitingExtractionContent(
    state: DocumentDetailState,
    contentPadding: PaddingValues,
    isLargeScreen: Boolean,
) {
    if (isLargeScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Constraints.Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            DokusCardSurface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                tech.dokus.features.cashflow.presentation.detail.PdfPreviewPane(
                    state = state.previewState,
                    selectedFieldPath = null,
                    onLoadMore = {},
                    modifier = Modifier.fillMaxSize(),
                    showScanAnimation = true
                )
            }
            DokusCardSurface(
                modifier = Modifier
                    .width(DesktopInspectorWidth)
                    .fillMaxHeight(),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cashflow_awaiting_extraction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            PreviewTabContent(
                previewState = state.previewState,
                showScanAnimation = true,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(Constraints.Spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_awaiting_extraction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun DesktopReviewContent(
    state: DocumentDetailState,
    isAccountantReadOnly: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentDetailIntent) -> Unit,
    onCorrectContact: (ResolvedContact) -> Unit,
    onCreateContact: (ResolvedContact) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constraints.Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        DesktopDocumentPane(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        ReviewInspectorPane(
            state = state,
            isAccountantReadOnly = isAccountantReadOnly,
            onIntent = onIntent,
            onCorrectContact = { onCorrectContact(state.effectiveContact) },
            onCreateContact = { onCreateContact(state.effectiveContact) },
            modifier = Modifier
                .width(DesktopInspectorWidth)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun DesktopDocumentPane(
    state: DocumentDetailState,
    onIntent: (DocumentDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceIds = state.documentRecord?.sources.orEmpty().map { it.id.toString() }.toSet()
    val activeSourceId = state.sourceViewerState?.sourceId?.toString()
    val activeTabId = activeSourceId?.takeIf { it in sourceIds } ?: SummaryTabId

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        CanonicalCenterPane(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        SourceTabsPanel(
            sources = state.documentRecord?.sources.orEmpty(),
            activeTabId = activeTabId,
            onTabSelected = { selectedTabId ->
                if (selectedTabId == activeTabId) return@SourceTabsPanel
                if (selectedTabId == SummaryTabId) {
                    onIntent(DocumentDetailIntent.Preview(DocumentPreviewIntent.CloseSourceModal))
                } else {
                    onIntent(DocumentDetailIntent.Preview(DocumentPreviewIntent.OpenSourceModal(DocumentSourceId.parse(selectedTabId))))
                }
            },
        )
    }
}

@Composable
private fun SourceTabsPanel(
    sources: List<DocumentSourceDto>,
    activeTabId: String,
    onTabSelected: (String) -> Unit,
) {
    if (sources.isEmpty()) return

    val tabs = sourceTabs(sources)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = SourceTabsPanelShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { tab ->
                    val isActive = tab.id == activeTabId
                    SourceTabChip(
                        label = tab.label,
                        isActive = isActive,
                        onClick = { onTabSelected(tab.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTabChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
    val activeBg = MaterialTheme.colorScheme.surfaceHover
    val inactiveBg = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    val textColor = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.textMuted

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = SourceTabShape,
        color = if (isActive) activeBg else inactiveBg,
        border = if (isActive) BorderStroke(1.dp, activeBorder) else null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class SourceTabItem(
    val id: String,
    val label: String,
)

@Composable
private fun sourceTabs(sources: List<DocumentSourceDto>): List<SourceTabItem> {
    val sourceChannelCounts = sources.groupingBy { it.sourceChannel }.eachCount()
    val sourceChannelIndices = mutableMapOf<tech.dokus.domain.enums.DocumentSource, Int>()

    return buildList {
        add(SourceTabItem(id = SummaryTabId, label = "Document"))
        sources.forEach { source ->
            val index = (sourceChannelIndices[source.sourceChannel] ?: 0) + 1
            sourceChannelIndices[source.sourceChannel] = index
            val hasDuplicates = (sourceChannelCounts[source.sourceChannel] ?: 0) > 1
            val label = if (hasDuplicates) {
                "${source.sourceChannel.localizedUppercase} $index"
            } else {
                source.sourceChannel.localizedUppercase
            }
            add(SourceTabItem(id = source.id.toString(), label = label))
        }
    }
}

@Composable
private fun MobileReviewContent(
    state: DocumentDetailState,
    isAccountantReadOnly: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentDetailIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val adjustedContentPadding = contentPadding.withTopRemoved(layoutDirection)

    if (state.shouldUsePdfFallback) {
        MobileFallbackContent(
            state = state,
            contentPadding = adjustedContentPadding,
            onBackClick = onBackClick,
        )
        return
    }

    MobileCanonicalContent(
        state = state,
        isAccountantReadOnly = isAccountantReadOnly,
        onIntent = onIntent,
        onBackClick = onBackClick,
        onOpenSource = onOpenSource,
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(adjustedContentPadding, layoutDirection)
            .imePadding()
            .dismissKeyboardOnTapOutside()
            .navigationBarsPadding(),
    )
}

@Composable
private fun MobileFallbackContent(
    state: DocumentDetailState,
    contentPadding: PaddingValues,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(contentPadding, androidx.compose.ui.platform.LocalLayoutDirection.current),
    ) {
        MobileDocumentDetailTopBar(
            state = state,
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(top = Constraints.Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            MobileCanonicalHeader(state = state)
            Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
            DokusCardSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Constraints.Spacing.small),
            ) {
                PreviewTabContent(
                    previewState = state.previewState,
                    showScanAnimation = state.isProcessing,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    state: DocumentDetailState,
    contentPadding: PaddingValues,
) {
    val errorState = state.document as? tech.dokus.foundation.app.state.DokusState.Error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constraints.Spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        if (errorState != null) {
            DokusErrorContent(
                exception = errorState.exception,
                retryHandler = errorState.retryHandler
            )
        }
    }
}

private fun PaddingValues.withTopRemoved(layoutDirection: LayoutDirection): PaddingValues = PaddingValues(
    start = calculateStartPadding(layoutDirection),
    top = 0.dp,
    end = calculateEndPadding(layoutDirection),
    bottom = calculateBottomPadding(),
)
