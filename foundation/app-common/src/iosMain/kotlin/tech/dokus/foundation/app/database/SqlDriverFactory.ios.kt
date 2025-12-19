package tech.dokus.foundation.app.database

import ai.dokus.foundation.domain.model.common.Feature
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    return NativeSqliteDriver(
        schema = schema.synchronous(),
        name = "${frontendDbName}.db"
    )
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun Feature.deleteSqlDatabase() {
    val fileManager = NSFileManager.defaultManager
    val documentDirectory = fileManager.URLsForDirectory(
        directory = NSDocumentDirectory,
        inDomains = NSUserDomainMask
    ).firstOrNull() ?: return

    val dbPath = (documentDirectory as? platform.Foundation.NSURL)?.path + "/${frontendDbName}.db"
    fileManager.removeItemAtPath(dbPath, null)
}
