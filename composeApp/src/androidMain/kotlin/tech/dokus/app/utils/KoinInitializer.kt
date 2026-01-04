package tech.dokus.app.utils

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

/**
 * Initializes Koin dependency injection for the application.
 *
 * Should be called once at application startup with all feature module DI modules.
 * Automatically stops existing Koin instance if present (useful for debug/testing).
 *
 * @param modules List of Koin modules from all features (collected from AppModule.diModules)
 * @param appDeclaration Optional platform-specific Koin configuration
 */
fun initKoin(
    modules: List<Module>,
    appDeclaration: KoinAppDeclaration = {}
): KoinApplication {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) {
        stopKoin()
    }
    return startKoin {
        appDeclaration()
        modules(modules)
    }
}
