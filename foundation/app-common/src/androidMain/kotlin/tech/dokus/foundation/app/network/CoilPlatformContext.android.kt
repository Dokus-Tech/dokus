package tech.dokus.foundation.app.network

import android.content.Context
import coil3.PlatformContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object CoilContextHolder : KoinComponent {
    val context: Context by inject()
}

internal actual fun coilPlatformContext(): PlatformContext = CoilContextHolder.context
