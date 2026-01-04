package tech.dokus.features.auth.initializer

import tech.dokus.features.auth.database.AuthDb
import tech.dokus.foundation.app.AppDataInitializer

internal class AuthDataInitializer(
    private val authDb: AuthDb
) : AppDataInitializer {
    override suspend fun initialize() {
        authDb.initialize()
    }
}
