package tech.dokus.foundation.aura.components.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints

@Immutable
data class DokusTableColumnSpec(
    val weight: Float? = null,
    val width: Dp? = null,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start
)

@Composable
fun DokusTableRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = 48.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(horizontal = Constraints.Spacing.large),
    cellSpacing: Dp = Constraints.Spacing.large,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = minHeight)
            .background(backgroundColor)
            .then(clickableModifier)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(cellSpacing),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun RowScope.DokusTableCell(
    column: DokusTableColumnSpec,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val columnModifier = when {
        column.width != null -> Modifier.width(column.width)
        column.weight != null -> Modifier.weight(column.weight)
        else -> Modifier
    }

    Box(
        modifier = columnModifier.then(modifier),
        contentAlignment = column.horizontalAlignment.toBoxAlignment(),
        content = content
    )
}

private fun Alignment.Horizontal.toBoxAlignment(): Alignment {
    return when (this) {
        Alignment.CenterHorizontally -> Alignment.Center
        Alignment.End -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
}
