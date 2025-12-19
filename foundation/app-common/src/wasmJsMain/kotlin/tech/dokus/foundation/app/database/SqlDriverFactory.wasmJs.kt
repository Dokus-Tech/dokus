package tech.dokus.foundation.app.database

import ai.dokus.foundation.domain.model.common.Feature
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver

actual suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    val driver = createDefaultWebWorkerDriver()
    schema.create(driver).await()
    return driver
}

actual suspend fun Feature.deleteSqlDatabase() {
    // IndexedDB will handle database recreation when driver is created next time
    // No explicit deletion needed for WASM
}
