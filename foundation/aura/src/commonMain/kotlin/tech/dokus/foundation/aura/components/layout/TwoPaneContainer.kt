package tech.dokus.foundation.aura.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * A simple shared container that draws a single middle effect once and lays out
 * left and right panes on top of it. This ensures the effect is shared visually
 * across both panes without duplication.
 */
@Composable
fun TwoPaneContainer(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    middleEffect: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isLarge = LocalScreenSize.isLarge

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Draw a shared background /effect only once
        middleEffect()

        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                left()
            }

            if (isLarge) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    right()
                }
            }
        }
    }
}

@Preview
@Composable
private fun TwoPaneContainerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        TwoPaneContainer(
            left = { Text("Left Pane") },
            right = { Text("Right Pane") },
        )
    }
}
