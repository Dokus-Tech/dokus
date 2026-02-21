package tech.dokus.foundation.aura.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
fun PIcon(
    icon: ImageVector,
    description: String?,
    isError: Boolean = false,
    tint: Color = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    },
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = modifier
    )
}

@Preview
@Composable
private fun PIconPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PIcon(icon = Icons.Default.Info, description = "Info")
    }
}
