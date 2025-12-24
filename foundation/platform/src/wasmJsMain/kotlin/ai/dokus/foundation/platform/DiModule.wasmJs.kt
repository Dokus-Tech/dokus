package ai.dokus.foundation.platform

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * WASM/Web implementation of networkMonitorModule.
 * Uses default constructor since browser APIs don't require context.
 */
actual val networkMonitorModule: Module = module {
    single { NetworkMonitor() }
}
