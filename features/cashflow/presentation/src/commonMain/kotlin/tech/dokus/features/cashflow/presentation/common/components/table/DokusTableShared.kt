package tech.dokus.features.cashflow.presentation.common.components.table

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.components.DokusCardSurface

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
internal fun DokusTableHeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun DokusTableChevronIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
