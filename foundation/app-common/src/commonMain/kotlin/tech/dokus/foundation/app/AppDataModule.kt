package tech.dokus.foundation.app

import org.koin.core.module.Module

interface AppDataModule {
    val dataDi: AppDataModuleDi

    /**
     * Initialize async resources (databases, etc).
     * Called after Koin initialization, before app usage.
     * Safe to call multiple times - implementations should be idempotent.
     */
    suspend fun initializeData() {}
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
