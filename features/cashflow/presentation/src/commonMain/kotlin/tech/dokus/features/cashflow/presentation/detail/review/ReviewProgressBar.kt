package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_next_issue
import tech.dokus.aura.resources.review_surface_step_of
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val BarHeight = 3.dp
private val ActiveBarWidth = 32.dp
private val InactiveBarWidth = 16.dp
private val BarShape = RoundedCornerShape(2.dp)

/**
 * Step progress indicator for multi-issue review.
 *
 * Shows small bars (active = wider + amber, inactive = grey),
 * "Step N of M" label, and "Next: [issue title]" right-aligned.
 *
 * Only rendered when there are 2+ issues.
 */
@Composable
internal fun ReviewProgressBar(
    currentStep: Int,
    totalSteps: Int,
    nextIssueTitle: String?,
    modifier: Modifier = Modifier,
) {
    if (totalSteps <= 1) return

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        // Progress bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalSteps) { index ->
                val isActive = index == currentStep
                Box(
                    modifier = Modifier
                        .width(if (isActive) ActiveBarWidth else InactiveBarWidth)
                        .height(BarHeight)
                        .clip(BarShape)
                        .background(
                            if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        // "Step 1 of 3"
        Text(
            text = stringResource(Res.string.review_surface_step_of, currentStep + 1, totalSteps),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )

        // Spacer to push "Next:" to the right
        Box(modifier = Modifier.weight(1f))

        // "Next: Verify amount"
        if (nextIssueTitle != null) {
            Text(
                text = stringResource(Res.string.review_surface_next_issue, nextIssueTitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}
