package ai.dokus.foundation.design.components.common

import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * @deprecated Use [Constrains.Breakpoint] instead for consistent breakpoint values.
 */
@Deprecated(
    message = "Use Constrains.Breakpoint instead",
    replaceWith = ReplaceWith(
        "Constrains.Breakpoint",
        "ai.dokus.foundation.design.constrains.Constrains"
    )
)
object Breakpoints {
    @Deprecated(
        message = "Use Constrains.Breakpoint.SMALL instead",
        replaceWith = ReplaceWith(
            "Constrains.Breakpoint.SMALL",
            "ai.dokus.foundation.design.constrains.Constrains"
        )
    )
    const val SMALL = Constrains.Breakpoint.SMALL

    @Deprecated(
        message = "Use Constrains.Breakpoint.LARGE instead",
        replaceWith = ReplaceWith(
            "Constrains.Breakpoint.LARGE",
            "ai.dokus.foundation.design.constrains.Constrains"
        )
    )
    const val LARGE = Constrains.Breakpoint.LARGE
}

/**
 * @deprecated Use [Constrains.limitWidth] or [Constrains.limitWidthCenteredContent] instead.
 */
@Deprecated(
    message = "Use Modifier.limitWidth() from Constrains instead",
    replaceWith = ReplaceWith(
        "limitWidth()",
        "ai.dokus.foundation.design.constrains.limitWidth"
    )
)
fun Modifier.limitedWidth(): Modifier = widthIn(max = Constrains.Breakpoint.SMALL.dp)
