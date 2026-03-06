package tech.dokus.foundation.app.local

import androidx.compose.runtime.staticCompositionLocalOf

val LocalBookkeeperConsoleCallback = staticCompositionLocalOf<(() -> Unit)?> { null }
