package ai.dokus.foundation.platform

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * JVM/Desktop implementation of networkMonitorModule.
 * Uses default constructor since no platform context is required.
 */
actual val networkMonitorModule: Module = module {
    single { NetworkMonitor() }
}
