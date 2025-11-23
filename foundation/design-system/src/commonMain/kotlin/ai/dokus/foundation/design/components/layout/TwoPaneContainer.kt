package ai.dokus.foundation.design.components.layout

import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
