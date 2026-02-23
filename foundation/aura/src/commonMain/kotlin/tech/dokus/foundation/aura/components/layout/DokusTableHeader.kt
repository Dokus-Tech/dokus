package tech.dokus.foundation.aura.components.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.borderStrong
import tech.dokus.foundation.aura.style.thText
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val DividerThickness = 1.5.dp

/**
 * Table column specification for [DokusTableHeader].
 */
@Immutable
data class DokusHeaderColumn(
    val label: String,
    val weight: Float? = null,
    val width: Dp? = null,
    val alignment: Alignment.Horizontal = Alignment.Start,
)

/**
 * Grid-based table column header row.
 *
 * @param columns Column definitions with label, weight/width, and alignment
 */
@Composable
fun DokusTableHeader(
    columns: List<DokusHeaderColumn>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Constraints.Table.headerPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEach { column ->
            HeaderCell(column)
        }
    }
    HorizontalDivider(
        thickness = DividerThickness,
        color = MaterialTheme.colorScheme.borderStrong,
    )
}

@Preview
@Composable
private fun DokusTableHeaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusTableHeader(
            columns = listOf(
                DokusHeaderColumn(label = "Date", weight = 1f),
                DokusHeaderColumn(label = "Description", weight = 2f),
                DokusHeaderColumn(label = "Amount", weight = 1f, alignment = Alignment.End)
            )
        )
    }
}

@Composable
private fun RowScope.HeaderCell(column: DokusHeaderColumn) {
    val textAlign = when (column.alignment) {
        Alignment.End -> TextAlign.End
        Alignment.CenterHorizontally -> TextAlign.Center
        else -> TextAlign.Start
    }

    val cellModifier = when {
        column.weight != null -> Modifier.weight(column.weight)
        column.width != null -> Modifier.width(column.width)
        else -> Modifier.weight(1f)
    }

    Text(
        text = column.label,
        modifier = cellModifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.thText,
        letterSpacing = 0.02.em,
        textAlign = textAlign,
        maxLines = 1,
    )
}
