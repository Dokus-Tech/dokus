package tech.dokus.features.cashflow.presentation.detail.components.bankstatement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_duplicate_badge
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun DuplicateBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.bank_statement_duplicate_badge),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
            .clip(RoundedCornerShape(Constraints.Spacing.xSmall))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
            .padding(
                horizontal = Constraints.Spacing.small,
                vertical = Constraints.Spacing.xxSmall,
            ),
    )
}
