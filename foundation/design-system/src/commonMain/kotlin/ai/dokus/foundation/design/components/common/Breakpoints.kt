package ai.dokus.foundation.design.components.common

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Breakpoints {
    const val SMALL = 600
    const val LARGE = 1200
}


fun Modifier.limitedWidth(): Modifier = widthIn(max = Breakpoints.SMALL.dp)