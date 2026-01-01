package tech.dokus.features.auth.database

import tech.dokus.foundation.app.database.DatabaseWrapper
import tech.dokus.domain.model.common.Feature

class AuthDb private constructor() : DatabaseWrapper<AuthDatabase> by DatabaseWrapper(
    feature = Feature.Auth,
    schema = AuthDatabase.Schema,
    createDatabase = { driver -> AuthDatabase(driver) }
) {
    companion object {
        fun create(): AuthDb = AuthDb()
    }
}