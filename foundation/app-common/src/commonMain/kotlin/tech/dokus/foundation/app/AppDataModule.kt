package tech.dokus.foundation.app

import org.koin.core.module.Module

interface AppDataModule {
    val dataDi: AppDataModuleDi
}

interface AppDataModuleDi {
    val platform: Module?
    val network: Module?
    val data: Module?
}

val AppDataModuleDi.allModules: List<Module>
    get() = listOfNotNull(
        platform,
        network,
        data,
    )
