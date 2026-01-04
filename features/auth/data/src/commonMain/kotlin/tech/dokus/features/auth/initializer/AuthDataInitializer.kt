package tech.dokus.features.auth.initializer

import tech.dokus.features.auth.database.AuthDb
import tech.dokus.foundation.app.AppDataInitializer

interface AuthDataInitializer : AppDataInitializer

internal class AuthDataInitializerImpl(
    private val authDb: AuthDb
) : AuthDataInitializer {
    override suspend fun initialize() {
        authDb.initialize()
    }
}
