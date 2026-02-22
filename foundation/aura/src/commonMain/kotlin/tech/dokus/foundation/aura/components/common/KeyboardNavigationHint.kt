package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_queue_navigate
import tech.dokus.foundation.aura.style.textFaint

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Keyboard navigation hint bar showing ↑ ↓ arrow keys and a "navigate" label.
 * Used in document queue sidebars.
 */
@Composable
fun KeyboardNavigationHint(modifier: Modifier = Modifier) {
    val faintColor = MaterialTheme.colorScheme.textFaint
    val monoFamily = MaterialTheme.typography.labelLarge.fontFamily

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\u2191",
            fontSize = 8.sp,
            fontFamily = monoFamily,
            color = faintColor,
            modifier = Modifier
                .border(1.dp, faintColor, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = "\u2193",
            fontSize = 8.sp,
            fontFamily = monoFamily,
            color = faintColor,
            modifier = Modifier
                .border(1.dp, faintColor, RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(Res.string.document_queue_navigate),
            fontSize = 9.sp,
            color = faintColor,
        )
    }
}

@Preview
@Composable
private fun KeyboardNavigationHintPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        KeyboardNavigationHint()
    }
}
