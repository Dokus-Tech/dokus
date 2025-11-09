package ai.dokus.app.core.database

import ai.dokus.foundation.domain.model.common.Feature
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    return NativeSqliteDriver(
        schema = schema.synchronous(),
        name = "${frontendDbName}.db"
    )
}

actual suspend fun Feature.deleteSqlDatabase() {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    documentDirectory?.path?.let { dir ->
        val dbPath = "$dir/${frontendDbName}.db"
        NSFileManager.defaultManager.removeItemAtPath(dbPath, null)
    }
}
