package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.comparison_reason_fuzzy_candidate
import tech.dokus.aura.resources.comparison_reason_material_conflict
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun ComparisonReasonBanner(
    reasonType: ReviewReason,
    modifier: Modifier = Modifier,
) {
    val text = when (reasonType) {
        ReviewReason.MaterialConflict -> stringResource(Res.string.comparison_reason_material_conflict)
        ReviewReason.FuzzyCandidate -> stringResource(Res.string.comparison_reason_fuzzy_candidate)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Constraints.Spacing.small))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
            .padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
