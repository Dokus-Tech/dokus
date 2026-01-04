package tech.dokus.foundation.app

/**
 * Initializes data-layer resources that require async setup (databases, caches, etc).
 */
interface AppDataInitializer {
    suspend fun initialize()
}
