package tech.dokus.foundation.app

import org.koin.core.module.Module

/** Domain layer setup - business logic and use cases */
interface AppDomainModule {
    val domainDi: AppDomainModuleDi
}

interface AppDomainModuleDi {
    val useCases: Module? // Business logic, validators
}

val AppDomainModuleDi.allModules: List<Module>
    get() = listOfNotNull(
        useCases
    )
