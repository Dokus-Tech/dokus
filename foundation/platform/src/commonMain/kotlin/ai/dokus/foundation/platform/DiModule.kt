package ai.dokus.foundation.platform

import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Platform DI module providing core infrastructure services.
 *
 * Includes:
 * - [Settings] - Multiplatform settings storage
 * - [Persistence] - User data persistence
 * - [NetworkMonitor] - Network connectivity monitoring (via platform-specific module)
 *
 * Note: ServerConfigManager and DynamicDokusEndpointProvider are registered
 * in the app's infrastructure module to avoid circular dependencies.
 */
val platformModule = module {
    includes(networkMonitorModule)

    // Settings storage
    single<Settings> { Settings() }

    // User persistence (uses settings)
    single<Persistence> { Persistence(get<Settings>()) }
}

/**
 * Platform-specific module that provides NetworkMonitor.
 * Android requires Context, other platforms use default constructors.
 */
expect val networkMonitorModule: Module
