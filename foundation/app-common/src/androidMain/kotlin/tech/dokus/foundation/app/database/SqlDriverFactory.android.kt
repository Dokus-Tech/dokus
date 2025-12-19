package tech.dokus.foundation.app.database

import ai.dokus.foundation.domain.model.common.Feature
import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.koin.mp.KoinPlatform.getKoin

actual suspend fun Feature.createSqlDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
    val context = getKoin().get<Context>()
    return AndroidSqliteDriver(
        schema = schema.synchronous(),
        context = context,
        name = "${frontendDbName}.db"
    )
}

actual suspend fun Feature.deleteSqlDatabase() {
    val context = getKoin().get<Context>()
    context.deleteDatabase("${frontendDbName}.db")
}
