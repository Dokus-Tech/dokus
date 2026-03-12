package tech.dokus.foundation.aura.components.filter

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Horizontally scrollable filter bar for placing filter groups
 * (tabs, dropdowns, toggles) in a single row.
 *
 * Scrolls horizontally when content overflows (typically on mobile).
 * On desktop, where content usually fits, the scroll is invisible.
 *
 * **Note:** Do not use `Modifier.weight()` inside this component —
 * it is incompatible with the unbounded horizontal scroll constraints.
 */
@Composable
fun DokusFilterBar(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Constraints.Spacing.medium),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
