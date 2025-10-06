package ai.dokus.app.core.constrains

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

val isLargeScreen: Boolean
    @Composable
    get() = LocalWindowInfo.current.containerSize.width.dp > Constrains.largeScreenWidth