package ai.dokus.foundation.platform

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android implementation of networkMonitorModule.
 * Provides NetworkMonitor with Android Context for ConnectivityManager access.
 */
actual val networkMonitorModule: Module = module {
    single { NetworkMonitor(androidContext()) }
}
