package tech.dokus.foundation.app.local

import androidx.compose.runtime.compositionLocalOf
import tech.dokus.foundation.app.AppModule

val LocalAppModules = compositionLocalOf { emptyList<AppModule>() }
