package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
fun PLeftPaneHeader(
    backLabel: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    backContentDescription: String? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.small
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PBackButton(
            modifier = if (backContentDescription == null) {
                Modifier
            } else {
                Modifier.semantics { contentDescription = backContentDescription }
            },
            label = backLabel,
            onBackPress = onBackClick
        )
        trailing()
    }
}
