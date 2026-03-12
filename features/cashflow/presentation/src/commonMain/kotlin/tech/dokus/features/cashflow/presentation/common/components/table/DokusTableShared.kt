package tech.dokus.features.cashflow.presentation.common.components.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun DokusTableSurface(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    DokusCardSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (header != null) {
                header()
                DokusTableDivider()
            }
            content()
        }
    }
}

@Composable
internal fun DokusTableDivider(
    modifier: Modifier = Modifier
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = modifier
    )
}

@Composable
internal fun DokusTableChevronIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Lucide.ChevronRight,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DokusTableSharedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusTableSurface(
            header = { Text("Column Header") }
        ) {
            DokusTableDivider()
            Text("Row Content")
        }
    }
}
