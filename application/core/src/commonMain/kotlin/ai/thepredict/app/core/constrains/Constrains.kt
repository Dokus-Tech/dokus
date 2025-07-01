package ai.thepredict.app.core.constrains

import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Constrains {
    val largeScreenWidth = 1400.dp
    val largeScreenHeight = 1000.dp
}

fun Modifier.limitWidth(): Modifier = widthIn(max = 980.dp)