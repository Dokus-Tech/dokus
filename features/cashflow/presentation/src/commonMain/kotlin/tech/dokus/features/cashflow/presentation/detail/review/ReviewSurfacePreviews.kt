@file:Suppress("LongMethod")

package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// =============================================================================
// Full Review Surface Previews
// =============================================================================

@Preview(name = "Review Surface - Clean", widthDp = 1080, heightDp = 760)
@Composable
private fun ReviewSurfaceCleanPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopReviewSurface(
            state = previewCleanReviewState(),
            isAccountantReadOnly = false,
            contentPadding = PaddingValues(0.dp),
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Review Surface - Contact Issue", widthDp = 1080, heightDp = 760)
@Composable
private fun ReviewSurfaceContactIssuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DesktopReviewSurface(
            state = previewContactSuggestedState(),
            isAccountantReadOnly = false,
            contentPadding = PaddingValues(0.dp),
            onIntent = {},
            onCorrectContact = {},
            onCreateContact = {},
            onSwitchToDetail = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

// =============================================================================
// Identity Block Previews
// =============================================================================

@Preview(name = "Identity Block")
@Composable
private fun ReviewIdentityBlockPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewIdentityBlock(
            vendorName = "Sky Hotel",
            totalAmount = "\u20AC2.589,70",
            dateDisplay = "2026-03-10",
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Document Card Previews
// =============================================================================

@Preview(name = "Document Card")
@Composable
private fun ReviewDocumentCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewDocumentCard(
            previewState = DocumentPreviewState.NoPreview,
            onZoomClick = {},
            modifier = Modifier.padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Progress Bar Previews
// =============================================================================

@Preview(name = "Progress Bar - Step 1 of 3")
@Composable
private fun ReviewProgressBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewProgressBar(
            currentStep = 0,
            totalSteps = 3,
            nextIssueTitle = "Verify VAT rate",
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Progress Bar - Step 2 of 3")
@Composable
private fun ReviewProgressBarStep2Preview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewProgressBar(
            currentStep = 1,
            totalSteps = 3,
            nextIssueTitle = "Confirm due date",
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Keyboard Hints Previews
// =============================================================================

@Preview(name = "Keyboard Hints - Can Confirm")
@Composable
private fun ReviewKeyboardHintsConfirmPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewKeyboardHints(
            canConfirm = true,
            modifier = Modifier.padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Keyboard Hints - Cannot Confirm")
@Composable
private fun ReviewKeyboardHintsNoConfirmPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewKeyboardHints(
            canConfirm = false,
            modifier = Modifier.padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Issue Card Previews
// =============================================================================

@Preview(name = "Contact Issue - Suggested")
@Composable
private fun ContactIssueSuggestedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ContactIssueCard(
            issue = previewContactIssueSuggested,
            onAcceptSuggestion = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Contact Issue - Unknown")
@Composable
private fun ContactIssueUnknownPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ContactIssueCard(
            issue = previewContactIssueUnknown,
            onAcceptSuggestion = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Contact Issue - Detected")
@Composable
private fun ContactIssueDetectedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ContactIssueCard(
            issue = previewContactIssueDetected,
            onAcceptSuggestion = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Amount Issue")
@Composable
private fun AmountIssuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        AmountIssueCard(
            issue = previewAmountIssue,
            totalValue = "",
            subtotalValue = "117.77",
            vatValue = "",
            onUpdateTotal = {},
            onUpdateSubtotal = {},
            onUpdateVat = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Date Issue - Due Before Issue")
@Composable
private fun DateIssueDueBeforePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DateIssueCard(
            issue = previewDateIssueDueBefore,
            onUpdateDueDate = {},
            onKeepOriginal = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Date Issue - Far Out")
@Composable
private fun DateIssueFarOutPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DateIssueCard(
            issue = previewDateIssueFarOut,
            onUpdateDueDate = {},
            onKeepOriginal = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Date Issue - Missing Due Date")
@Composable
private fun DateIssueMissingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DateIssueCard(
            issue = previewDateIssueMissing,
            onUpdateDueDate = {},
            onKeepOriginal = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Direction Issue")
@Composable
private fun DirectionIssuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DirectionIssueCard(
            issue = previewDirectionIssue,
            onSelectDirection = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Action Footer Previews
// =============================================================================

@Preview(name = "Action Footer - Confirm")
@Composable
private fun ActionFooterConfirmPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewActionFooter(
            actionType = ReviewActionType.Confirm,
            isEnabled = true,
            isLoading = false,
            showChooseDifferent = false,
            onPrimaryAction = {},
            onChooseDifferent = {},
            onReviewLater = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Action Footer - Accept & Continue")
@Composable
private fun ActionFooterAcceptContinuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewActionFooter(
            actionType = ReviewActionType.AcceptAndContinue,
            isEnabled = true,
            isLoading = false,
            showChooseDifferent = false,
            onPrimaryAction = {},
            onChooseDifferent = {},
            onReviewLater = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Action Footer - Loading")
@Composable
private fun ActionFooterLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewActionFooter(
            actionType = ReviewActionType.AcceptAndConfirm,
            isEnabled = true,
            isLoading = true,
            showChooseDifferent = false,
            onPrimaryAction = {},
            onChooseDifferent = {},
            onReviewLater = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

// =============================================================================
// Decision Stream Previews
// =============================================================================

@Preview(name = "Decision Stream - Clean")
@Composable
private fun DecisionStreamCleanPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewDecisionStream(
            state = previewCleanReviewState(),
            issues = emptyList(),
            activeIssueIndex = 0,
            onIntent = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Decision Stream - Single Contact Issue")
@Composable
private fun DecisionStreamSingleIssuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewDecisionStream(
            state = previewContactSuggestedState(),
            issues = listOf(previewContactIssueSuggested),
            activeIssueIndex = 0,
            onIntent = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}

@Preview(name = "Decision Stream - Multiple Issues")
@Composable
private fun DecisionStreamMultiIssuePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ReviewDecisionStream(
            state = previewContactSuggestedState(),
            issues = listOf(
                previewContactIssueSuggested,
                previewAmountIssue,
                previewDateIssueFarOut,
            ),
            activeIssueIndex = 0,
            onIntent = {},
            onChooseDifferent = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
        )
    }
}
