package ai.dokus.foundation.design.constrains

import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object Constrains {
    val largeScreenWidth = 980.dp
    val largeScreenDefaultWidth = 1280.dp
    val largeScreenHeight = 840.dp
}

fun Modifier.limitWidth(): Modifier = widthIn(max = 980.dp)
fun Modifier.limitWidthCenteredContent(): Modifier = widthIn(max = 360.dp)

@Composable
fun Modifier.withContentPaddingForScrollable(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(Modifier.padding(top = 16.dp).then(Modifier.padding(horizontal = 32.dp)))
    }
    return then(Modifier.padding(horizontal = 16.dp))
}

@Composable
fun Modifier.withContentPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(Modifier.padding(vertical = 16.dp, horizontal = 32.dp))
    }
    return then(Modifier.padding(horizontal = 16.dp))
}

@Composable
fun Modifier.withHorizontalPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(Modifier.padding(horizontal = 32.dp))
    }
    return then(Modifier.padding(horizontal = 16.dp))
}

@Composable
fun Modifier.withExtraTopPaddingMobile(): Modifier {
    if (LocalScreenSize.isLarge) return this
    return then(Modifier.padding(top = 16.dp))
}

@Composable
fun ContentPaddingVertical() {
    Spacer(modifier = Modifier.padding(vertical = 16.dp))
}