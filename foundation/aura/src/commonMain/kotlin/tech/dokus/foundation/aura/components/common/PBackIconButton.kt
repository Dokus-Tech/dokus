package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val BackButtonSize = 40.dp
private val BackIconSize = 18.dp
private const val BackButtonSurfaceAlpha = 0.52f
private const val BackButtonBorderAlpha = 0.58f

@Composable
fun PBackIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Surface(
        modifier = modifier.size(BackButtonSize),
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = BackButtonSurfaceAlpha),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = BackButtonBorderAlpha),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = contentDescription,
                modifier = Modifier.size(BackIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun PBackIconButtonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PBackIconButton(onClick = {})
    }
}

@Preview(name = "Back Icon Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun PBackIconButtonDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PBackIconButton(onClick = {})
    }
}
