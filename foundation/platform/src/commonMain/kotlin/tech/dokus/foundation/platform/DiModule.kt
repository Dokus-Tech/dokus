package tech.dokus.foundation.platform

import com.russhwolf.settings.Settings
import org.koin.dsl.module

/**
 * Platform DI module providing core infrastructure services.
 *
 * Includes:
 * - [Settings] - Multiplatform settings storage
 * - [Persistence] - User data persistence
 *
 * Note: ServerConfigManager and DynamicDokusEndpointProvider are registered
 * in the app's infrastructure module to avoid circular dependencies.
 */
val platformModule = module {
    // Settings storage
    single<Settings> { Settings() }

    // User persistence (uses settings)
    single<Persistence> { Persistence(get<Settings>()) }
}
