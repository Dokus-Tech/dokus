package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_duplicate_diff_confirm
import tech.dokus.aura.resources.review_duplicate_diff_opinion
import tech.dokus.aura.resources.review_duplicate_different_document
import tech.dokus.aura.resources.review_duplicate_existing
import tech.dokus.aura.resources.review_duplicate_impact_update
import tech.dokus.aura.resources.review_duplicate_incoming
import tech.dokus.aura.resources.review_duplicate_label_invoice
import tech.dokus.aura.resources.review_duplicate_label_issue_date
import tech.dokus.aura.resources.review_duplicate_label_total
import tech.dokus.aura.resources.review_duplicate_possible_duplicate
import tech.dokus.aura.resources.review_duplicate_review_later
import tech.dokus.aura.resources.review_duplicate_same_document
import tech.dokus.aura.resources.review_duplicate_same_opinion
import tech.dokus.aura.resources.review_duplicate_same_opinion_detail
import tech.dokus.aura.resources.review_surface_view_full_detail
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.DuplicateDiff
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewState
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val ThumbnailShape = RoundedCornerShape(6.dp)
private const val A4_ASPECT_RATIO = 0.75f

/**
 * Desktop duplicate comparison surface.
 *
 * Shows two document identity cards side by side with inline diffs
 * and "Same / Different" action buttons. Uses typed [DuplicateReviewState]
 * with two [DocDto] objects — no JSON parsing.
 */
@Composable
internal fun DesktopDuplicateReviewSurface(
    state: DuplicateReviewState,
    contentPadding: PaddingValues,
    onResolveSame: () -> Unit,
    onResolveDifferent: () -> Unit,
    onSwitchToDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) {
        Box(
            modifier = modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            DokusLoader()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
                ) {
                    StatusDot(type = StatusDotType.Warning, size = 8.dp)
                    Text(
                        stringResource(Res.string.review_duplicate_possible_duplicate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable(onClick = onSwitchToDetail)
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier.weight(1f).widthIn(max = 700.dp)
                    .align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Two document cards
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(Constraints.Spacing.large)) {
                    DocumentIdentityCard(
                        label = stringResource(Res.string.review_duplicate_existing),
                        labelColor = MaterialTheme.colorScheme.tertiary,
                        borderColor = MaterialTheme.colorScheme.tertiary,
                        uiData = state.existingUiData,
                        previewState = state.existingPreview,
                        statusLabel = "Confirmed",
                        statusColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    DocumentIdentityCard(
                        label = stringResource(Res.string.review_duplicate_incoming),
                        labelColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary,
                        uiData = state.incomingUiData,
                        previewState = state.incomingPreview,
                        statusLabel = "Processing",
                        statusColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Diff section
                DuplicateDiffSection(
                    reasonType = state.reasonType ?: ReviewReason.MaterialConflict,
                    diffs = state.diffs,
                )

                // Actions
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(Constraints.Spacing.medium)) {
                    PButton(
                        text = stringResource(Res.string.review_duplicate_different_document),
                        variant = PButtonVariant.OutlineMuted,
                        isEnabled = !state.isResolving,
                        onClick = onResolveDifferent,
                    )
                    PButton(
                        text = stringResource(Res.string.review_duplicate_same_document),
                        isEnabled = !state.isResolving,
                        isLoading = state.isResolving,
                        modifier = Modifier.weight(1f),
                        onClick = onResolveSame,
                    )
                }

                Text(
                    stringResource(Res.string.review_duplicate_review_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable { })
            }

            // Bottom bar
            BottomBar(onSwitchToDetail)
        }
    }
}

@Composable
private fun BottomBar(onSwitchToDetail: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
    ) {
        ReviewKeyboardHints(canConfirm = true)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = Constraints.Stroke.thin
        )
        Text(
            stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.clickable(onClick = onSwitchToDetail)
                .padding(vertical = Constraints.Spacing.small)
        )
    }
}

// =============================
// Document Identity Card
// =============================

@Composable
private fun DocumentIdentityCard(
    label: String,
    labelColor: Color,
    borderColor: Color,
    uiData: DocumentUiData?,
    previewState: DocumentPreviewState,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    val imageLoader = rememberAuthenticatedImageLoader()
    val vendorName = when (uiData) {
        is DocumentUiData.Invoice -> uiData.primaryDescription
        is DocumentUiData.CreditNote -> uiData.primaryDescription
        is DocumentUiData.Receipt -> uiData.primaryDescription
        else -> ""
    }
    val invoiceNumber = when (uiData) {
        is DocumentUiData.Invoice -> uiData.invoiceNumber ?: ""
        is DocumentUiData.CreditNote -> uiData.creditNoteNumber ?: ""
        else -> ""
    }
    val totalAmount = when (uiData) {
        is DocumentUiData.Invoice -> uiData.totalAmount?.let { "${uiData.currencySign}${it.formatAmount()}" }
            ?: ""

        is DocumentUiData.CreditNote -> uiData.totalAmount?.let { "${uiData.currencySign}${it.formatAmount()}" }
            ?: ""

        is DocumentUiData.Receipt -> uiData.totalAmount?.let { "${uiData.currencySign}${it.formatAmount()}" }
            ?: ""

        else -> ""
    }
    val dateDisplay = when (uiData) {
        is DocumentUiData.Invoice -> uiData.issueDate ?: ""
        is DocumentUiData.CreditNote -> uiData.issueDate ?: ""
        is DocumentUiData.Receipt -> uiData.date ?: ""
        else -> ""
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )

        Surface(
            modifier = Modifier.fillMaxWidth().aspectRatio(A4_ASPECT_RATIO),
            shape = ThumbnailShape, shadowElevation = 4.dp, tonalElevation = 0.dp,
            border = BorderStroke(2.dp, borderColor.copy(alpha = 0.2f)),
        ) {
            when (previewState) {
                is DocumentPreviewState.Ready -> {
                    val url = previewState.pages.firstOrNull()?.imageUrl
                    if (url != null) {
                        SubcomposeAsyncImage(
                            model = url, contentDescription = null, imageLoader = imageLoader,
                            loading = {
                                FallbackCard(
                                    vendorName,
                                    invoiceNumber,
                                    totalAmount,
                                    dateDisplay
                                )
                            },
                            error = {
                                FallbackCard(
                                    vendorName,
                                    invoiceNumber,
                                    totalAmount,
                                    dateDisplay
                                )
                            },
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                    } else FallbackCard(vendorName, invoiceNumber, totalAmount, dateDisplay)
                }

                is DocumentPreviewState.Loading -> Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center
                ) { DokusLoader(size = DokusLoaderSize.Small) }

                is DocumentPreviewState.Error, is DocumentPreviewState.NotPdf, is DocumentPreviewState.NoPreview ->
                    FallbackCard(vendorName, invoiceNumber, totalAmount, dateDisplay)
            }
        }

        Text(
            statusLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun FallbackCard(
    vendorName: String,
    invoiceNumber: String,
    totalAmount: String,
    dateDisplay: String
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF5F0E8)), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
        ) {
            Text(
                vendorName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
            if (invoiceNumber.isNotBlank()) Text(
                "#$invoiceNumber",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                totalAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF111111)
            )
            Text(dateDisplay, style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888))
        }
    }
}

// =============================
// Diff Section
// =============================

@Composable
private fun DuplicateDiffSection(
    reasonType: ReviewReason,
    diffs: List<DuplicateDiff>,
    modifier: Modifier = Modifier
) {
    val reasonTitle = when (reasonType) {
        ReviewReason.MaterialConflict -> "AMOUNT CHANGED"
        ReviewReason.FuzzyCandidate -> "POSSIBLE MATCH"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)
    ) {
        IssueTitleLabel(text = reasonTitle)

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
        ) {
            StatusDot(
                type = if (diffs.isEmpty()) StatusDotType.Confirmed else StatusDotType.Warning,
                size = 6.dp
            )
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                Text(
                    if (diffs.isEmpty()) stringResource(Res.string.review_duplicate_same_opinion) else stringResource(
                        Res.string.review_duplicate_diff_opinion
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (diffs.isEmpty()) stringResource(Res.string.review_duplicate_same_opinion_detail) else stringResource(
                        Res.string.review_duplicate_diff_confirm
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        if (diffs.isNotEmpty()) {
            val amberColor = MaterialTheme.colorScheme.primary
            Column(
                Modifier.fillMaxWidth().drawBehind {
                    drawLine(
                        amberColor,
                        Offset(0f, 0f),
                        Offset(0f, size.height),
                        2.dp.toPx()
                    )
                }.padding(start = Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)
            ) {
                diffs.forEach { diff ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(
                            when (diff.field) {
                                "total" -> stringResource(Res.string.review_duplicate_label_total); "invoiceNo" -> stringResource(
                                Res.string.review_duplicate_label_invoice
                            ); "issueDate" -> stringResource(Res.string.review_duplicate_label_issue_date); else -> diff.field
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)) {
                            Text(
                                diff.existingValue,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                            Text(
                                "\u2192",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                            Text(
                                diff.incomingValue,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            val totalDiff = diffs.firstOrNull { it.field == "total" }
            if (totalDiff != null) {
                Text(
                    stringResource(
                        Res.string.review_duplicate_impact_update,
                        totalDiff.existingValue,
                        totalDiff.incomingValue
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }
    }
}

// DocumentUiData.primaryDescription gives vendor/merchant name for display
