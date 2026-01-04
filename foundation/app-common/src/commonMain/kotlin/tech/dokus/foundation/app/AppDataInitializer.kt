package tech.dokus.foundation.app

/**
 * Initializes data-layer resources that require async setup (databases, caches, etc).
 */
interface AppDataInitializer {
    suspend fun initialize()

    companion object {
        operator fun invoke(vararg initializers: AppDataInitializer): AppDataInitializer {
            return AppDataInitializerManager(initializers.toList())
        }
    }
}

private class AppDataInitializerManager(
    private val dataInitializers: List<AppDataInitializer>
) : AppDataInitializer {
    override suspend fun initialize() {
        dataInitializers.forEach { it.initialize() }
    }
}