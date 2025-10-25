package ai.dokus.app.core.local

import ai.dokus.app.core.AppModule
import androidx.compose.runtime.compositionLocalOf

val LocalAppModules = compositionLocalOf { emptyList<AppModule>() }
