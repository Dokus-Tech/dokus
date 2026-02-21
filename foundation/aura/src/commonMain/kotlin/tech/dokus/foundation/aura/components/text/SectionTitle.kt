package tech.dokus.foundation.aura.components.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Section header with optional right-side action.
 *
 * Style: 14sp, weight 700, onSurface color.
 * Uses: "Recent" on Today, "Previous periods" on Accountant, section headers everywhere.
 *
 * @param text Title text
 * @param right Optional right-side composable (link, count, badge)
 * @param onBackPress Optional back button
 */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    onBackPress: (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
) {
    val arrangement = if (right != null) Arrangement.SpaceBetween else horizontalArrangement

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = arrangement,
        modifier = modifier
            .fillMaxWidth()
            .then(Modifier.padding(bottom = Constraints.Spacing.medium)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            if (onBackPress != null) {
                PBackButton(onBackPress = onBackPress)
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (right != null) {
            right()
        }
    }
}

@Preview
@Composable
private fun SectionTitlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SectionTitle(text = "Recent Activity")
    }
}
