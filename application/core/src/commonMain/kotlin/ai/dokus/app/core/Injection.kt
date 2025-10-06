package ai.dokus.app.core

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Initialize Koin dependency injection
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    return startKoin {
        appDeclaration()
    }
}

/**
 * Configure and initialize Koin with modules
 */
fun configureDi(vararg modules: Module) {
    initKoin {
        modules(*modules)
    }
}