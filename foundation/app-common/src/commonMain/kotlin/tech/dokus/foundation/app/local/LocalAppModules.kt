package tech.dokus.foundation.app.local

import tech.dokus.foundation.app.AppModule
import androidx.compose.runtime.compositionLocalOf

val LocalAppModules = compositionLocalOf { emptyList<AppModule>() }
