package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_accept_and_confirm
import tech.dokus.aura.resources.review_surface_accept_and_continue
import tech.dokus.aura.resources.review_surface_choose_different
import tech.dokus.aura.resources.review_surface_confirm
import tech.dokus.aura.resources.review_surface_save_and_confirm
import tech.dokus.aura.resources.review_surface_save_and_continue
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * The type of primary action button to display.
 */
internal enum class ReviewActionType {
    /** No issues — plain Confirm */
    Confirm,
    /** Contact/amount accepted — Accept & confirm (last issue) */
    AcceptAndConfirm,
    /** Contact/amount accepted — Accept & continue (more issues) */
    AcceptAndContinue,
    /** Date edited — Save & confirm (last issue) */
    SaveAndConfirm,
    /** Date edited — Save & continue (more issues) */
    SaveAndContinue,
}

/**
 * Primary action button + optional "Choose different" + "Review later" text links.
 *
 * Order matches mockup: CTA → "Choose different" → "Review later"
 */
@Composable
internal fun ReviewActionFooter(
    actionType: ReviewActionType,
    isEnabled: Boolean,
    isLoading: Boolean,
    showChooseDifferent: Boolean,
    onPrimaryAction: () -> Unit,
    onChooseDifferent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        PButton(
            text = actionType.label(),
            isEnabled = isEnabled,
            isLoading = isLoading,
            modifier = Modifier.fillMaxWidth(),
            onClick = onPrimaryAction,
        )

        if (showChooseDifferent) {
            Text(
                text = stringResource(Res.string.review_surface_choose_different),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.clickable(onClick = onChooseDifferent),
            )
        }
    }
}

@Composable
private fun ReviewActionType.label(): String = when (this) {
    ReviewActionType.Confirm -> stringResource(Res.string.review_surface_confirm)
    ReviewActionType.AcceptAndConfirm -> stringResource(Res.string.review_surface_accept_and_confirm)
    ReviewActionType.AcceptAndContinue -> stringResource(Res.string.review_surface_accept_and_continue)
    ReviewActionType.SaveAndConfirm -> stringResource(Res.string.review_surface_save_and_confirm)
    ReviewActionType.SaveAndContinue -> stringResource(Res.string.review_surface_save_and_continue)
}

/**
 * Derive the action type from the current issue state.
 */
internal fun resolveActionType(
    issues: List<ReviewIssue>,
    activeIndex: Int,
): ReviewActionType {
    if (issues.isEmpty()) return ReviewActionType.Confirm

    val activeIssue = issues.getOrNull(activeIndex) ?: return ReviewActionType.Confirm
    val isLastIssue = activeIndex >= issues.lastIndex

    return when (activeIssue) {
        is ReviewIssue.ContactIssue -> if (isLastIssue) ReviewActionType.AcceptAndConfirm else ReviewActionType.AcceptAndContinue
        is ReviewIssue.DirectionIssue -> if (isLastIssue) ReviewActionType.AcceptAndConfirm else ReviewActionType.AcceptAndContinue
        is ReviewIssue.AmountIssue -> if (isLastIssue) ReviewActionType.SaveAndConfirm else ReviewActionType.SaveAndContinue
        is ReviewIssue.DateIssue -> if (isLastIssue) ReviewActionType.SaveAndConfirm else ReviewActionType.SaveAndContinue
    }
}
