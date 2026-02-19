package tech.dokus.foundation.aura.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints

enum class ScreenSize {
    SMALL,
    MEDIUM,
    LARGE;

    internal companion object {
        fun fromWidthDp(widthDp: Dp): ScreenSize {
            return when {
                widthDp >= Constraints.Breakpoint.LARGE.dp -> LARGE
                widthDp >= Constraints.Breakpoint.SMALL.dp -> MEDIUM
                else -> SMALL
            }
        }
    }

    val isLarge: Boolean
        get() = this == LARGE

    val isMedium: Boolean
        get() = this == MEDIUM

    val isSmall: Boolean
        get() = this == SMALL
}

val LocalScreenSize = staticCompositionLocalOf { ScreenSize.SMALL }

val ProvidableCompositionLocal<ScreenSize>.isLarge: Boolean
    @Composable get() = current.isLarge

val ProvidableCompositionLocal<ScreenSize>.isMedium: Boolean
    @Composable get() = current.isMedium

@Composable
fun ScreenSizeProvided(content: @Composable () -> Unit) {
    val containerWidth = LocalWindowInfo.current.containerSize.width
    val density = LocalDensity.current

    val containerWidthDp = remember(density, containerWidth) {
        with(density) {
            containerWidth.toDp()
        }
    }

    val screenSize by remember(containerWidthDp) {
        derivedStateOf { ScreenSize.fromWidthDp(containerWidthDp) }
    }

    CompositionLocalProvider(LocalScreenSize provides screenSize) {
        content()
    }
}
