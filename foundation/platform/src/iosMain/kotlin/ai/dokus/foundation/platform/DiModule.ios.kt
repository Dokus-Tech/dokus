package ai.dokus.foundation.platform

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS implementation of networkMonitorModule.
 * Uses default constructor since NWPathMonitor doesn't require context.
 */
actual val networkMonitorModule: Module = module {
    single { NetworkMonitor() }
}
